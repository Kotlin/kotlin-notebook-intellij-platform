package org.jetbrains.kotlinx.jupyter.intellij

import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import org.jetbrains.kotlinx.jupyter.api.libraries.dependencies
import org.jetbrains.kotlinx.jupyter.intellij.util.FilteringSetBuilder
import org.jetbrains.kotlinx.jupyter.intellij.util.findPackagesInJarOrDirectory
import org.jetbrains.kotlinx.jupyter.intellij.util.getAllIntellijPaths

@JupyterLibrary
class IntellijJupyterIntegration : JupyterIntegration() {
    override fun Builder.onLoaded() {
        if (!notebook.kernelRunMode.isRunInsideIntellijProcess) {
            throw IllegalStateException("Jupyter integration should be loaded inside the IDE process only")
        }

        importPackage<IntellijJupyterIntegration>()

        val pathsToAdd = getAllIntellijPaths()

        addDependenciesAndImports(pathsToAdd, ::isIntellijImport)
    }

    private fun isIntellijImport(import: String): Boolean {
        return import.startsWith("com.intellij.")
    }

    private fun Builder.addDependenciesAndImports(
        pathsToAdd: Set<String>,
        filterImports: (String) -> Boolean = { true },
    ) {
        dependencies {
            for (jarPath in pathsToAdd) {
                implementation(jarPath)
            }
        }

        val starImports =
            FilteringSetBuilder(
                filter = filterImports,
                modifier = { "$it.*" },
            ).apply {
                for (jarPath in pathsToAdd) {
                    findPackagesInJarOrDirectory(jarPath, this)
                }
            }.build()

        for (import in starImports) {
            import(import)
        }
    }
}
