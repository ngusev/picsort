package com.uasoft.picsort

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.mov.QuickTimeDirectory
import com.drew.metadata.mp4.Mp4Directory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.io.path.*

typealias LogCallback = (message: String) -> Unit

class PicSorter(
    private val sourceDir: Path,
    private val targetDir: Path,
    private val logger: Logger,
    private val fileHandler: FileHandler,
    private val onMessage: LogCallback = {}
) {
    companion object {
        fun prepareAndSort(
            sourceDir: Path, parentTargetDir: Path, onMessage: LogCallback = {}
        ): PicSorter {
            val dtfLog = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")
            val nowStr = LocalDateTime.now().format(dtfLog)
            val targetDir = parentTargetDir.resolve(nowStr)
            Files.createDirectories(targetDir)

            val logFile = targetDir.resolve("sorter.log").toFile()
            val logger = Logger.getLogger("PicSorterLogger")
            logger.useParentHandlers = false
            val fileHandler = FileHandler(logFile.absolutePath)
            fileHandler.formatter = object : java.util.logging.Formatter() {
                override fun format(record: LogRecord): String {
                    return "${formatMessage(record)}${if (record.thrown != null) "\n${record.thrown}" else ""}\n"
                }
            }
            logger.addHandler(fileHandler)

            logger.info("Starting sort at $nowStr")
            return PicSorter(sourceDir, targetDir, logger, fileHandler, onMessage)
        }
    }

    private val dtf = DateTimeFormatter.ofPattern("yyyy/yyyy-MM-dd").withZone(ZoneId.systemDefault())
    private val earliestInstant = Instant.parse("1990-01-01T00:00:00Z")
    private val knownExtensions = setOf(
        "gif",
        "png",
        "flv",
        "avi",
        "wmv",
        "bmp",
        "rw2",
        "cr2",
        "nef",
        "3gp",
        "m2ts",
        "m4v",
        "mts",
        "heic",
        "heif",
        "webp",
        "avif"
    )

    private val processedCount = AtomicInteger(0)
    private val failedCount = AtomicInteger(0)
    private val foundExtensions = ConcurrentSkipListSet<String>()

    private fun report(message: String) {
        logger.info(message)
        onMessage(message)
    }

    suspend fun sort() = withContext(Dispatchers.IO) {
        try {
            // Walk the tree folder-by-folder, processing each folder's files as we reach it.
            // No upfront counting pass — we just walk and report as we go.
            val dirs = Files.walk(sourceDir).use { it.filter(Files::isDirectory).toList() }
            var fileCount = 0
            for (dir in dirs) {
                val rel = sourceDir.relativize(dir).toString().ifEmpty { sourceDir.fileName.toString() }
                report("Folder: $rel")
                val files = Files.list(dir).use { stream ->
                    stream.filter(Files::isRegularFile).toList()
                }
                files.map { path -> async { handleFile(path) } }.awaitAll()
                fileCount += files.size
                report("  processed $fileCount files")
            }

            report("Result: folders>${dirs.size} files>$fileCount processed>${processedCount.get()} failed>${failedCount.get()}")
            if (foundExtensions.isNotEmpty()) {
                report("Unknown extensions: $foundExtensions")
            }
        } finally {
            fileHandler.close()
            logger.removeHandler(fileHandler)
        }
    }

    private fun handleFile(path: Path) {
        try {
            when (val ext = path.extension.lowercase(Locale.getDefault())) {
                "jpeg", "jpg", "jif", "jfif", "heic", "heif" -> processWithTimestamp(
                    path,
                    ExifSubIFDDirectory::class.java,
                    ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL,
                    targetDir.resolve("pictures")
                )

                "mpeg", "mpg", "mp4" -> processWithTimestamp(
                    path, Mp4Directory::class.java, Mp4Directory.TAG_CREATION_TIME, targetDir.resolve("video")
                )

                "mov" -> processWithTimestamp(
                    path,
                    QuickTimeDirectory::class.java,
                    QuickTimeDirectory.TAG_CREATION_TIME,
                    targetDir.resolve("video")
                )

                else -> {
                    if (knownExtensions.contains(ext)) {
                        copyToOther(path)
                        processedCount.incrementAndGet()
                    } else {
                        logger.warning("not processed: $path")
                        if (ext.length < 5) foundExtensions.add(ext)
                        failedCount.incrementAndGet()
                    }
                }
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, path.toString(), e)
            copyToOther(path)
            failedCount.incrementAndGet()
        }
    }

    private fun <T : Directory> processWithTimestamp(path: Path, dirType: Class<T>, tag: Int, targetPath: Path) {
        val metadata = ImageMetadataReader.readMetadata(path.toFile())
        val directory = metadata.getFirstDirectoryOfType(dirType)
        if (directory == null) {
            logger.warning("directory is null: $path")
            copyToOther(path)
            failedCount.incrementAndGet()
            return
        }
        val date = directory.getDate(tag)
        if (date == null) {
            val attr = Files.readAttributes(path, BasicFileAttributes::class.java)
            logger.warning("no date: $path > c: ${attr.creationTime()}, m: ${attr.lastModifiedTime()}")
            copyToOther(path)
            failedCount.incrementAndGet()
            return
        }
        if (date.toInstant().isBefore(earliestInstant)) {
            val attr = Files.readAttributes(path, BasicFileAttributes::class.java)
            logger.warning("wrong date: $path > $date > c: ${attr.creationTime()}, m: ${attr.lastModifiedTime()}")
            copyToTarget(targetPath.resolve("wrong_date"), path, date)
            failedCount.incrementAndGet()
        } else {
            copyToTarget(targetPath, path, date)
            processedCount.incrementAndGet()
        }
    }

    private fun copyToOther(path: Path) {
        val dir = targetDir.resolve("other").resolve(sourceDir.relativize(path.parent))
        dir.createDirectories()
        copyWithDedup(path, dir)
    }

    private fun copyToTarget(targetPath: Path, path: Path, date: Date) {
        val dir = targetPath.resolve(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(dtf))
        dir.createDirectories()
        copyWithDedup(path, dir)
    }

    private fun copyWithDedup(source: Path, targetDir: Path) {
        var dest = targetDir.resolve(source.name)
        var counter = 0
        while (true) {
            try {
                Files.copy(source, dest)
                if (counter > 0) {
                    logger.info("Renamed duplicate: ${source.name} -> ${dest.name}")
                }
                return
            } catch (_: FileAlreadyExistsException) {
                counter++
                dest = targetDir.resolve("${source.nameWithoutExtension}_$counter.${source.extension}")
            }
        }
    }
}
