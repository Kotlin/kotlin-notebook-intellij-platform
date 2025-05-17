package org.jetbrains.kotlinx.jupyter.intellij.utils

fun collectSuitableStarImports(pathsToAdd: Set<String>): Set<String> {
    return FilteringSetBuilder(
        filter = ::isIntellijImport,
        modifier = { "$it.*" },
    ).apply {
        for (jarPath in pathsToAdd) {
            findPackagesInJarOrDirectory(
                jarPath,
                this,
                dontAddPackageIfItContainsClassifier = { forbiddenIntellijClassifierNames.contains(it) },
            )
        }
    }.build()
}

private fun isIntellijImport(import: String): Boolean {
    return import.startsWith("com.intellij.")
}

private val forbiddenIntellijClassifierNames =
    setOf(
        "String",
        "Int",
        "Double",
        "Float",
        "Boolean",
        "Integer",
    )
