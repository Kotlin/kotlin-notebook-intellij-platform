package org.jetbrains.kotlinx.jupyter.intellij

import com.jetbrains.plugin.structure.ide.ProductInfoBasedIde
import com.jetbrains.plugin.structure.ide.createIde
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode.SKIP_SILENTLY
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParser
import jupyter.kotlin.ScriptTemplateWithDisplayHelpers
import jupyter.kotlin.USE
import org.jetbrains.kotlinx.jupyter.api.KotlinKernelHost
import org.jetbrains.kotlinx.jupyter.api.Notebook
import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import org.jetbrains.kotlinx.jupyter.api.libraries.createLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.dependencies
import org.jetbrains.kotlinx.jupyter.api.textResult
import org.jetbrains.kotlinx.jupyter.intellij.utils.getIntelliJPlatformPath
import kotlin.io.path.exists
import kotlin.io.path.pathString

private const val ERROR_INCOMPATIBLE_MODE = "IntelliJ Platform integration should be loaded inside the IDE process only"

val idePath by lazy {
    getIntelliJPlatformPath()
}

val productInfo by lazy {
    val parser = ProductInfoParser()
    val file = requireNotNull(idePath.resolve("Resources/product-info.json").takeIf { it.exists() })
    requireNotNull(parser.parse(file))
}

val ide by lazy {
    requireNotNull(createIde {
        missingLayoutFileMode = SKIP_SILENTLY
        path = idePath
    } as? ProductInfoBasedIde)
}

fun ScriptTemplateWithDisplayHelpers.loadBundledPlugins(vararg pluginIds: String) = USE {
    val jars = pluginIds.asSequence()
        .mapNotNull { ide.findPluginById(it) ?: ide.findPluginByModule(it) }
        .flatMap { it.classpath.paths }
        .toSet()

    USE {
        dependencies {
            jars.forEach {
                implementation(it.pathString)
            }
        }
    }
}

@JupyterLibrary
class IntelliJPlatformJupyterIntegration : JupyterIntegration() {

    override fun Builder.onLoaded() {
        onLoaded {
            onIntegrationLoaded(notebook)
        }
    }

    private fun KotlinKernelHost.onIntegrationLoaded(notebook: Notebook) {
        if (!notebook.kernelRunMode.isRunInsideIntellijProcess) {
            return displayError(ERROR_INCOMPATIBLE_MODE)
        }

        val intelliJPlatformJars = productInfo.launch
            ?.firstOrNull()
            ?.bootClassPathJarNames
            .orEmpty()
            .map { idePath.resolve("lib/$it") }
            .toSet()

        addLibrary(
            createLibrary(notebook) {
                importPackage<IntelliJPlatformJupyterIntegration>()
                import("org.jetbrains.kotlinx.jupyter.intellij.api.*")

                dependencies {
                    intelliJPlatformJars.forEach {
                        implementation(it.pathString)
                    }
                }
            },
        )
    }

    private fun KotlinKernelHost.displayError(message: String) =
        display(textResult(message), null)
}
