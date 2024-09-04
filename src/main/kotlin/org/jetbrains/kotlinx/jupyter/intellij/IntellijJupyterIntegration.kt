package org.jetbrains.kotlinx.jupyter.intellij

import org.jetbrains.kotlinx.jupyter.api.KotlinKernelHost
import org.jetbrains.kotlinx.jupyter.api.Notebook
import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import org.jetbrains.kotlinx.jupyter.api.libraries.createLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.dependencies
import org.jetbrains.kotlinx.jupyter.api.textResult
import org.jetbrains.kotlinx.jupyter.intellij.util.FilteringSetBuilder
import org.jetbrains.kotlinx.jupyter.intellij.util.findPackagesInJarOrDirectory
import org.jetbrains.kotlinx.jupyter.intellij.util.getAllIntellijPaths

@JupyterLibrary
class IntellijJupyterIntegration : JupyterIntegration() {
    override fun Builder.onLoaded() {
        onLoaded {
            onIntegrationLoaded(notebook)
        }
    }

    private fun KotlinKernelHost.onIntegrationLoaded(notebook: Notebook) {
        if (!notebook.kernelRunMode.isRunInsideIntellijProcess) {
            display(incompatibleRunModeResult(), null)
            return
        }
        addLibrary(
            createLibrary(notebook) {
                importPackage<IntellijJupyterIntegration>()
                import("org.jetbrains.kotlinx.jupyter.intellij.api.*")

                val pathsToAdd = getAllIntellijPaths()
                addDependenciesAndImports(pathsToAdd, ::isIntellijImport)
            },
        )
    }

    private fun incompatibleRunModeResult() = textResult("IntelliJ SDK integration should be loaded inside the IDE process only")

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
