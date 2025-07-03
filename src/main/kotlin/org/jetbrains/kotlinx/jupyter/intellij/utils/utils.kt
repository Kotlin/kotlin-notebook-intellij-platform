package org.jetbrains.kotlinx.jupyter.intellij.utils

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.outputStream
import kotlin.streams.asSequence

@OptIn(ExperimentalPathApi::class)
internal fun Path.extract(targetDirectory: Path) {
    targetDirectory.apply {
        deleteRecursively()
        createDirectories()
    }

    ZipFile(toFile()).use { zip ->
        zip.entries().asSequence().forEach { entry ->
            val outFile = targetDirectory.resolve(entry.name)
            when {
                entry.isDirectory -> outFile.createDirectories()
                else -> {
                    outFile.parent.createDirectories()
                    zip.getInputStream(entry).use {
                        it.copyTo(outFile.outputStream())
                    }
                }
            }
        }
    }
}

private val matchers by lazy {
    FileSystems.getDefault().let {
        listOf(
            it.getPathMatcher("glob:**/lib/*.jar"),
            it.getPathMatcher("glob:**/lib/modules/*.jar"),
        )
    }
}

internal fun Path.collectJars() =
    Files.walk(this)
        .asSequence()
        .filter { path -> Files.isRegularFile(path) }
        .filter { path -> matchers.any { it.matches(path) } }
        .toList()
