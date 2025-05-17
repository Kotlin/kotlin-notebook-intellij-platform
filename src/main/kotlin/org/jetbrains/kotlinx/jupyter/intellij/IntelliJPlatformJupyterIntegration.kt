package org.jetbrains.kotlinx.jupyter.intellij

import org.jetbrains.kotlinx.jupyter.api.KotlinKernelHost
import org.jetbrains.kotlinx.jupyter.api.Notebook
import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import org.jetbrains.kotlinx.jupyter.api.libraries.createLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.dependencies
import org.jetbrains.kotlinx.jupyter.api.textResult
import org.jetbrains.kotlinx.jupyter.intellij.utils.collectSuitableStarImports
import org.jetbrains.kotlinx.jupyter.intellij.utils.getAllIntellijPaths

@JupyterLibrary
class IntelliJPlatformJupyterIntegration : JupyterIntegration() {
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
                importPackage<IntelliJPlatformJupyterIntegration>()
                import("org.jetbrains.kotlinx.jupyter.intellij.api.*")

                val pathsToAdd = getAllIntellijPaths()
                addDependenciesAndImports(pathsToAdd)
            },
        )
    }

    private fun incompatibleRunModeResult() = textResult("IntelliJ Platform integration should be loaded inside the IDE process only")

    private fun Builder.addDependenciesAndImports(pathsToAdd: Set<String>) {
        dependencies {
            for (jarPath in pathsToAdd) {
                implementation(jarPath)
            }
        }

        val starImports = collectSuitableStarImports(pathsToAdd)

        for (import in starImports) {
            import(import)
        }
    }
}
