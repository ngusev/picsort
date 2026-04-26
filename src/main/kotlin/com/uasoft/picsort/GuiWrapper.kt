package com.uasoft.picsort

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.singleWindowApplication
import io.github.vinceglb.filekit.core.FileKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI
import java.nio.file.Paths

object GuiLauncher {
    @JvmStatic
    fun main(args: Array<String>) = singleWindowApplication(title = "PicSort") {
        MaterialTheme {
            PicSortApp()
        }
    }
}

@Composable
fun PicSortApp() {
    var sourcePath by remember { mutableStateOf<String?>(null) }
    var targetPath by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("") }
    var sorting by remember { mutableStateOf(false) }
    var outputLog by remember { mutableStateOf("") }
    val logBuilder = remember { StringBuilder() }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(onClick = {
            scope.launch {
                val dir = FileKit.pickDirectory(title = "Select Source Folder")
                if (dir != null) sourcePath = dir.path
            }
        }) {
            Text("Select Source Folder")
        }
        Text(sourcePath?.let { "Source: $it" } ?: "Source: (none)")

        Button(onClick = {
            scope.launch {
                val dir = FileKit.pickDirectory(title = "Select Target Folder")
                if (dir != null) targetPath = dir.path
            }
        }) {
            Text("Select Target Folder")
        }
        Text(targetPath?.let { "Target: $it" } ?: "Target: (none)")

        Button(
            onClick = {
                val src = sourcePath
                val tgt = targetPath
                if (src != null && tgt != null) {
                    sorting = true
                    progress = 0f
                    logBuilder.clear()
                    logBuilder.append("Starting sort...\nSource: $src\nTarget: $tgt\n")
                    outputLog = logBuilder.toString()
                    statusText = "Sorting..."
                    scope.launch {
                        val sorter = PicSorter.prepareAndSort(
                            Paths.get(src), Paths.get(tgt)
                        ) { processed, total ->
                            val interval = (total / 100).coerceAtLeast(1)
                            if (processed == total || processed % interval == 0) {
                                scope.launch {
                                    progress = processed.toFloat() / total
                                    statusText = "$processed / $total files"
                                    logBuilder.append("Processed $processed / $total files\n")
                                    outputLog = logBuilder.toString()
                                }
                            }
                        }
                        withContext(Dispatchers.IO) { sorter.sort() }
                        statusText = "Sorting complete."
                        logBuilder.append("Sorting complete.\n")
                        outputLog = logBuilder.toString()
                        sorting = false
                    }
                } else {
                    statusText = "Please select both directories."
                    outputLog = "Please select both directories.\n"
                }
            }, enabled = !sorting
        ) {
            Text("Start Sorting")
        }

        if (sorting) {
            LinearProgressIndicator(
                progress = { progress }, modifier = Modifier.fillMaxWidth()
            )
        }

        if (statusText.isNotEmpty()) {
            Text(statusText)
        }

        Text("Output", style = MaterialTheme.typography.labelMedium)
        val scrollState = rememberScrollState()
        LaunchedEffect(outputLog) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = MaterialTheme.shapes.small,
            tonalElevation = 1.dp,
            border = ButtonDefaults.outlinedButtonBorder(true)
        ) {
            SelectionContainer {
                Text(
                    text = outputLog.ifEmpty { " " },
                    modifier = Modifier.padding(8.dp).verticalScroll(scrollState),
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                )
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PicSort 1.0.2", style = MaterialTheme.typography.bodySmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { openUrl("https://github.com/ngusev/picsort") },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Star on GitHub", style = MaterialTheme.typography.bodySmall)
                }
                Image(
                    painter = painterResource("kofi.webp"),
                    contentDescription = "Buy me a coffee",
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier.height(24.dp).clickable { openUrl("https://ko-fi.com/ngusev") }
                )
            }
        }
    }
}

private fun openUrl(url: String) {
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(URI(url))
    }
}