package org.jetbrains.kotlinx.jupyter.intellij

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.jetbrains.plugin.structure.ide.ProductInfoBasedIde
import com.jetbrains.plugin.structure.ide.createIde
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode.SKIP_SILENTLY
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParser
import jupyter.kotlin.ScriptTemplateWithDisplayHelpers
import jupyter.kotlin.USE
import org.jetbrains.intellij.pluginRepository.PluginRepository
import org.jetbrains.intellij.pluginRepository.PluginRepositoryFactory
import org.jetbrains.kotlinx.jupyter.api.KotlinKernelHost
import org.jetbrains.kotlinx.jupyter.api.Notebook
import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import org.jetbrains.kotlinx.jupyter.api.libraries.createLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.dependencies
import org.jetbrains.kotlinx.jupyter.api.textResult
import org.jetbrains.kotlinx.jupyter.intellij.api.currentEditor
import org.jetbrains.kotlinx.jupyter.intellij.utils.*
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.pathString

private const val ERROR_INCOMPATIBLE_MODE = "IntelliJ Platform integration should be loaded inside the IDE process only"

val idePath: Path by lazy {
    getIntelliJPlatformPath()
}

val ide: ProductInfoBasedIde by lazy {
    requireNotNull(createIde {
        missingLayoutFileMode = SKIP_SILENTLY
        path = idePath
    } as? ProductInfoBasedIde)
}

val pluginRepository: PluginRepository by lazy {
    PluginRepositoryFactory.create("https://plugins.jetbrains.com")
}

val productInfo: ProductInfo by lazy {
    val parser = ProductInfoParser()
    val file = requireNotNull(idePath.resolve("Resources/product-info.json").takeIf { it.exists() })
    requireNotNull(parser.parse(file))
}

fun ScriptTemplateWithDisplayHelpers.loadBundledPlugins(vararg pluginIds: String): Unit = USE {
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

fun ScriptTemplateWithDisplayHelpers.loadPlugins(vararg pluginIds: String): Unit = USE {
    val pluginManager = PluginManager.getInstance()

    val jars = pluginIds.flatMap { pluginId ->
        val plugin = pluginManager.findEnabledPlugin(PluginId.getId(pluginId))
        requireNotNull(plugin)

        plugin.pluginPath.collectJars()
    }

    dependencies {
        jars.forEach {
            implementation(it.pathString)
        }
    }
}

fun ScriptTemplateWithDisplayHelpers.downloadPlugin(pluginId: String): Path? {
    // TODO: do not resolve KN file's parent directory that way, but KTNB-1035
    val storage = currentEditor()?.file?.parent?.toNioPath()?.resolve(".intellijPlatform/kotlinNotebook")
        ?.createDirectories()
        ?: createTempDirectory()

    val platformType = productInfo.productCode
    val platformVersion = productInfo.buildNumber

    val pluginRequest = PluginRequest.parse(pluginId)

    val id = pluginRequest.id
    val channel = pluginRequest.channel
    val version = pluginRequest.version
        ?: pluginRequest.resolveCompatibleVersion(platformType, platformVersion)
        ?: error("Failed to resolve version for plugin '$id'")

    val name = "$id-$version"
    val pluginDirectory = storage.resolve(name)

    if (!pluginDirectory.exists()) {
        val pluginArchive = storage.resolve("$name.zip")
        pluginRepository.downloader.download(id, version, pluginArchive.toFile(), channel)
        requireNotNull(pluginArchive) { "Failed to download plugin '$id' version '$version' from '$channel'" }
        pluginArchive.extract(pluginDirectory)
    }

    return pluginDirectory
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
