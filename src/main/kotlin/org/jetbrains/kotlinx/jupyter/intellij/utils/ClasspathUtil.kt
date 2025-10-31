package org.jetbrains.kotlinx.jupyter.intellij.utils

import java.nio.file.Path
import kotlin.io.path.name

private val unwantedClasspathEntryNameInfixes =
    listOf(
        /*
         * Dataframe compiler plugin accidentally contains Jupyter descriptor
         * that is loaded by kernel and breaks the session state
         */
        "dataframe-compiler-plugin",
        "compilerPlugins.dataframe",
    )

internal fun Iterable<Path>.excludeUnwantedClasspathEntries(): Set<Path> {
    return filterTo(mutableSetOf()) {
        val fileName = it.name
        unwantedClasspathEntryNameInfixes.none { infix ->
            fileName.contains(infix)
        }
    }
}
