package com.uasoft.picsort

import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

fun main(args: Array<String>) = runBlocking {
    if (args.size != 2) {
        println("Usage: picsort-cli <sourceDir> <targetDir>")
        return@runBlocking
    }

    val sourceDir = Paths.get(args[0])
    val targetDir = Paths.get(args[1])

    println("Starting sort from $sourceDir to $targetDir")
    PicSorter.prepareAndSort(sourceDir, targetDir) { processed, total ->
        print("\r$processed / $total files")
    }.sort()
    println("\nSorting complete.")
}
