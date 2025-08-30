// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlinx.jupyter.intellij

import com.intellij.jupyter.core.jupyter.connections.action.JupyterRestartKernelListener
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlinx.jupyter.api.KotlinKernelHost
import org.jetbrains.kotlinx.jupyter.api.Notebook
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import org.jetbrains.kotlinx.jupyter.api.libraries.createLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.dependencies
import org.jetbrains.kotlinx.jupyter.api.textResult
import org.jetbrains.kotlinx.jupyter.intellij.utils.IntelliJPlatformClassloader
import org.jetbrains.kotlinx.jupyter.intellij.utils.devMode.getAllIntellijPathsForDevMode
import org.jetbrains.kotlinx.jupyter.intellij.utils.toVersion
import org.jetbrains.kotlinx.jupyter.protocol.api.logger
import org.jetbrains.kotlinx.jupyter.util.ModifiableParentsClassLoader
import java.net.URLClassLoader
import java.nio.file.Path
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

            val productVersion = ApplicationInfo.getInstance().run { "$majorVersion.$minorVersion" }
            if (productVersion.toVersion() >= MINIMAL_SUPPORTED_IDE_VERSION) {
                initializeDisposable(notebook)
                initializeIntelliJPlatformClassloader(notebook)
            } else {
                val productName = ApplicationNamesInfo.getInstance().fullProductName
                error(
                    """
                    You are running $productName $productVersion.
                    The `notebookDisposable` and `loadPlugins()` are not fully supported in this version of the IDE.
                    Please upgrade to $MINIMAL_SUPPORTED_IDE_VERSION or higher for the full IntelliJ Platform integration experience.
                    """.trimIndent(),
                )
            }
        }
    }

    private fun KotlinKernelHost.loadIntelliJPlatform(notebook: Notebook) {
        val intelliJPlatformJars = getPlatformJars(notebook)

        addLibrary(
            createLibrary(notebook) {
                importPackage<IntelliJPlatformJupyterIntegration>()

                dependencies {
                    for (jarPath in intelliJPlatformJars) {
                        implementation(jarPath.invariantSeparatorsPathString)
                    }
                }
            },
        )

        displayText("IntelliJ Platform integration is loaded")
    }

    private fun getPlatformJars(notebook: Notebook): Set<Path> {
        val logger = notebook.loggerFactory.logger<IntelliJPlatformJupyterIntegration>()
        return when (val productInfo = productInfoOrNull) {
            null -> {
                logger.info("The product-info.json file isn't found, falling back to dev mode JARs detection")
                getAllIntellijPathsForDevMode()
            }
            else -> {
                logger.info("The product-info.json file is found, using JAR paths from it")
                productInfo.platformJars
            }
        }
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
        val workingDir = notebook.workingDir.toUri().toURL()

        base.addParent(URLClassLoader(arrayOf(workingDir)))
        base.addParent(intelliJPlatformClassLoader)
    }

    private fun KotlinKernelHost.displayText(message: String) = display(textResult(message), null)
}
