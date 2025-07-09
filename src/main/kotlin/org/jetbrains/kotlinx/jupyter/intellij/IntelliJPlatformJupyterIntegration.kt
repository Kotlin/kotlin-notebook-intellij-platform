package org.jetbrains.kotlinx.jupyter.intellij

import com.intellij.jupyter.core.jupyter.connections.action.JupyterRestartKernelListener
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlinx.jupyter.api.KotlinKernelHost
import org.jetbrains.kotlinx.jupyter.api.Notebook
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import org.jetbrains.kotlinx.jupyter.api.libraries.createLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.dependencies
import org.jetbrains.kotlinx.jupyter.api.textResult
import org.jetbrains.kotlinx.jupyter.intellij.utils.IntelliJPlatformClassloader
import org.jetbrains.kotlinx.jupyter.intellij.utils.toVersion
import org.jetbrains.kotlinx.jupyter.util.ModifiableParentsClassLoader
import kotlin.io.path.invariantSeparatorsPathString

/**
 * Represents a class loader that loads classes from the IntelliJ Platform.
 */
internal val intelliJPlatformClassLoader: IntelliJPlatformClassloader by lazy { IntelliJPlatformClassloader() }

/**
 * Represents the minimal supported version of the IDE required for running all parts of the IntelliJ Platform integration.
 */
private val MINIMAL_SUPPORTED_IDE_VERSION = "2025.2".toVersion()

/**
 * Represents a Jupyter integration for the IntelliJ Platform.
 */
class IntelliJPlatformJupyterIntegration : JupyterIntegration() {
    override fun Builder.onLoaded() {
        onLoaded {
            if (!notebook.kernelRunMode.isRunInsideIntellijProcess) {
                error("IntelliJ Platform integration can be run in the IDE process only")
            }

            loadIntelliJPlatform(notebook)

            if (productInfo.version.toVersion() >= MINIMAL_SUPPORTED_IDE_VERSION) {
                initializeDisposable(notebook)
                initializeIntelliJPlatformClassloader(notebook)
            } else {
                error(
                    """
                    You are running ${productInfo.name} ${productInfo.version}.
                    The `notebookDisposable` and `loadPlugins()` are not fully supported in this version of the IDE.
                    Please upgrade to 2025.1.3 or higher for the full IntelliJ Platform integration experience.
                    """.trimIndent(),
                )
            }
        }
    }

    private fun KotlinKernelHost.loadIntelliJPlatform(notebook: Notebook) {
        val intelliJPlatformJars =
            productInfo.launch
                ?.firstOrNull()
                ?.bootClassPathJarNames
                .orEmpty()
                .map { idePath.resolve("lib/$it") }
                .toSet()

        addLibrary(
            createLibrary(notebook) {
                importPackage<IntelliJPlatformJupyterIntegration>()

                dependencies {
                    intelliJPlatformJars.forEach {
                        implementation(it.invariantSeparatorsPathString)
                    }
                }
            },
        )

        displayText("IntelliJ Platform integration is loaded")
    }

    private fun KotlinKernelHost.initializeDisposable(notebook: Notebook) {
        requireNotNull(currentProjectFromNotebook(notebook))
            .messageBus
            .connect(notebookDisposable)
            .subscribe(
                JupyterRestartKernelListener.TOPIC,
                JupyterRestartKernelListener {
                    Disposer.dispose(notebookDisposable)
                    displayText("IntelliJ Platform integration is disposed")
                },
            )
    }

    private fun initializeIntelliJPlatformClassloader(notebook: Notebook) {
        val base = notebook.intermediateClassLoader as ModifiableParentsClassLoader
        base.addParent(intelliJPlatformClassLoader)
    }

    private fun KotlinKernelHost.displayText(message: String) = display(textResult(message), null)
}
