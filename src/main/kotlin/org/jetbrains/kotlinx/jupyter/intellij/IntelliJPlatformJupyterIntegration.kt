package org.jetbrains.kotlinx.jupyter.intellij

import com.intellij.ide.plugins.PluginMainDescriptor
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
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import org.jetbrains.kotlinx.jupyter.api.libraries.createLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.dependencies
import org.jetbrains.kotlinx.jupyter.api.textResult
import org.jetbrains.kotlinx.jupyter.intellij.utils.*
import org.jetbrains.kotlinx.jupyter.util.ModifiableParentsClassLoader
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.pathString

private const val ERROR_INCOMPATIBLE_MODE = "IntelliJ Platform integration should be loaded inside the IDE process only"

/**
 * Represents the resolved file system path to the IntelliJ Platform installation directory.
 */
val idePath: Path by lazy {
    getIntelliJPlatformPath()
}

/**
 * Represents an IDE instance, which allows interacting with plugins and resolve their dependencies based on the IDE's configuration.
 */
val ide: ProductInfoBasedIde by lazy {
    requireNotNull(createIde {
        missingLayoutFileMode = SKIP_SILENTLY
        path = idePath
    } as? ProductInfoBasedIde)
}

/**
 * Provides a lazily initialized instance of [PluginRepository], which is responsible for
 * managing plugins via the JetBrains Marketplace plugin repository.
 */
val pluginRepository: PluginRepository by lazy {
    PluginRepositoryFactory.create("https://plugins.jetbrains.com")
}


/**
 * Provides a lazily initialized instance of the [PluginManager], which allows accessing information about plugins
 * and their states in the current IDE environment.
 */
val pluginManager: PluginManager by lazy {
    PluginManager.getInstance()
}

/**
 * Lazily initialized property containing information about the current IntelliJ Platform product.
 */
val productInfo: ProductInfo by lazy {
    val parser = ProductInfoParser()
    val file = requireNotNull(idePath.resolve("Resources/product-info.json").takeIf { it.exists() })
    requireNotNull(parser.parse(file))
}

/**
 * Loads and integrates the specified bundled plugins into the current script context.
 *
 * @param pluginIds A list of plugin IDs to be loaded. Each identifier can represent either the plugin ID or the module ID.
 * @return Unit
 */
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

/**
 * Loads the plugin installed in the current IDE into the script context based on its plugin ID.
 * This method also supports optionally loading plugin classes and class loaders.
 *
 * @param pluginId A plugin ID to load.
 * @param loadClasses If true, the method loads the plugin classes into the current context. Defaults to true.
 * @param loadClassLoader If true, the method loads the plugin class loaders into the current context. Defaults to true.
 * @return A list of [PluginMainDescriptor] objects representing the requested plugins.
 * @throws IllegalArgumentException If any specified plugin ID does not match an enabled plugin in the current IDE.
 */
@Suppress("UnstableApiUsage")
fun ScriptTemplateWithDisplayHelpers.loadPlugin(
    pluginId: String,
    loadClasses: Boolean = true,
    loadClassLoader: Boolean = true,
): PluginMainDescriptor = loadPlugins(
    pluginId,
    loadClasses = loadClasses,
    loadClassLoader = loadClassLoader,
).first()

/**
 * Loads plugins installed in the current IDE into the script context based on their plugin IDs.
 * This method also supports optionally loading plugin classes and class loaders.
 *
 * @param pluginIds A list of plugin IDs to load.
 * @param loadClasses If true, the method loads the plugin classes into the current context. Defaults to true.
 * @param loadClassLoader If true, the method loads the plugin class loaders into the current context. Defaults to true.
 * @return A list of [PluginMainDescriptor] objects representing the requested plugins.
 * @throws IllegalArgumentException If any specified plugin ID does not match an enabled plugin in the current IDE.
 */
@Suppress("UnstableApiUsage")
fun ScriptTemplateWithDisplayHelpers.loadPlugins(
    vararg pluginIds: String,
    loadClasses: Boolean = true,
    loadClassLoader: Boolean = true,
): List<PluginMainDescriptor> = pluginIds
    .asSequence()
    .map { pluginId ->
        requireNotNull(pluginManager.findEnabledPlugin(PluginId.getId(pluginId)) as? PluginMainDescriptor) {
            "Plugin '$pluginId' is not found in the current IDE"
        }
    }
    .onEach { plugin ->
        if (loadClasses) {
            USE {
                dependencies {
                    plugin.pluginPath.collectJars().forEach { path ->
                        implementation(path.pathString)
                    }
                }
            }
        }
    }
    .onEach { plugin ->
        if (loadClassLoader) {
            // TODO: We should add only CLs of "leaf" modules - those not serving as a parent for other modules
            val base = userHandlesProvider.notebook.intermediateClassLoader as ModifiableParentsClassLoader

            plugin.content.modules
                .asSequence()
                .map { it.descriptor.classLoader }
                .onEach { base.addParent(it) }
        }
    }
    .toList()

/**
 * Downloads a plugin based on the specified plugin identifier, resolves its version, and extracts it, if necessary.
 * The plugin is stored in a dedicated directory under the working directory of the notebook.
 *
 * The [pluginId] can be specified in one of the following formats:
 * - `pluginId`
 * - `pluginId@channel`
 * - `pluginId:version`
 * - `pluginId:version@channel`
 *
 * Note that this method does not install the plugin in the IDE or load it into the current script context.
 *
 * @param pluginId A plugin ID to be downloaded.
 * @return The path to the directory where the downloaded plugin was extracted, or null if any issue occurs during the process.
 */
fun ScriptTemplateWithDisplayHelpers.downloadPlugin(pluginId: String): Path? {
    val storage = userHandlesProvider.notebook.workingDir
        .resolve(".intellijPlatform/kotlinNotebook")
        .createDirectories()

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

/**
 * Represents a Jupyter integration for the IntelliJ Platform.
 */
class IntelliJPlatformJupyterIntegration : JupyterIntegration() {

    override fun Builder.onLoaded() {
        onLoaded {
            onIntegrationLoaded(notebook)
        }
    }

    private fun KotlinKernelHost.onIntegrationLoaded(notebook: Notebook) {
        if (!notebook.kernelRunMode.isRunInsideIntellijProcess) {
            return displayText(ERROR_INCOMPATIBLE_MODE)
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
            }
        )

        displayText("IntelliJ Platform integration is loaded")
    }

    private fun KotlinKernelHost.displayText(message: String) =
        display(textResult(message), null)
}
