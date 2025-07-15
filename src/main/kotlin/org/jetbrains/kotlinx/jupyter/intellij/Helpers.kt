@file:Suppress("unused")

package org.jetbrains.kotlinx.jupyter.intellij

//import org.jetbrains.kotlinx.dataframe.AnyFrame
//import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import com.intellij.ide.plugins.PluginMainDescriptor
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.extensions.PluginId
import jupyter.kotlin.ScriptTemplateWithDisplayHelpers
import org.jetbrains.kotlinx.jupyter.api.exceptions.ReplUnwrappedExceptionImpl
import org.jetbrains.kotlinx.jupyter.api.libraries.dependencies
import org.jetbrains.kotlinx.jupyter.intellij.utils.PluginRequest
import org.jetbrains.kotlinx.jupyter.intellij.utils.collectJars
import org.jetbrains.kotlinx.jupyter.intellij.utils.extract
import org.jetbrains.kotlinx.jupyter.intellij.utils.resolveCompatibleVersion
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString

/**
 * Throws an [ReplUnwrappedExceptionImpl] with the specified [message].
 */
fun error(message: String): Nothing = throw ReplUnwrappedExceptionImpl(message)

/**
 * Loads and integrates the specified bundled plugins into the current script context.
 *
 * @param pluginIds A list of plugin IDs to be loaded. Each identifier can represent either the plugin ID or the module ID.
 * @return Unit
 */
fun ScriptTemplateWithDisplayHelpers.loadBundledPlugins(vararg pluginIds: String): Unit =
    USE {
        val jars =
            pluginIds.asSequence()
                .mapNotNull { ide.findPluginById(it) ?: ide.findPluginByModule(it) }
                .flatMap { it.classpath.paths }
                .toSet()

        USE {
            dependencies {
                for (jar in jars) {
                    implementation(jar.invariantSeparatorsPathString)
                }
            }
        }
    }

/**
 * Loads plugins installed in the current IDE into the script context based on their plugin IDs.
 * This method also supports optionally loading plugin classes and class loaders.
 *
 * @param pluginIds A list of plugin IDs to load.
 * @param loadClasses If true, the method loads the plugin classes into the current context. Defaults to true.
 * @param loadClassLoader If true, the method loads the plugin class loaders into the current context. Defaults to true.
 * @throws IllegalArgumentException If any specified plugin ID does not match an enabled plugin in the current IDE.
 */
@Suppress("UnstableApiUsage", "unused")
fun ScriptTemplateWithDisplayHelpers.loadPlugins(
    vararg pluginIds: String,
    loadClasses: Boolean = true,
    loadClassLoader: Boolean = true,
) {
    pluginIds
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
                        for (path in plugin.pluginPath.collectJars()) {
                            implementation(path.invariantSeparatorsPathString)
                        }
                    }
                }
            }
        }
        .onEach { plugin ->
            if (loadClassLoader) {
                val pluginClassloaders =
                    plugin.content.modules
                        .map { it.descriptor.classLoader } + listOf(plugin.classLoader)

                intelliJPlatformClassLoader.addParents(pluginClassloaders)
            }
        }
        .toList()
}

///**
// * Prints the list of installed plugins in the currently run IDE.
// */
//fun ScriptTemplateWithDisplayHelpers.printPlugins(): AnyFrame {
//    USE {
//        EXECUTE("%use dataframe")
//    }
//
//    return with(PluginManagerCore.plugins) {
//        mapOf(
//            "ID" to map { it.pluginId },
//            "name" to map { it.name },
//            "version" to map { it.version },
//            "vendor" to map { it.vendor },
//            "bundled" to map { it.isBundled },
//            "loaded" to map { PluginManagerCore.isLoaded(it.pluginId) },
//            "enabled" to map { !PluginManagerCore.isDisabled(it.pluginId) },
//        )
//    }.toDataFrame()
//}

/**
 * Runs the given [block] in the EDT.
 * If an exception is thrown, it is displayed in the output.
 */
@Suppress("UnstableApiUsage")
fun ScriptTemplateWithDisplayHelpers.runInEdt(block: () -> Unit): Unit =
    runCatching {
        invokeAndWaitIfNeeded {
            block()
        }
    }.onFailure {
        error(it.cause?.message.orEmpty())
    }.getOrNull() ?: Unit

/**
 * TODO: Hidden from public as there's no real use-case for it yet.
 *
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
private fun ScriptTemplateWithDisplayHelpers.downloadPlugin(pluginId: String): Path? {
    val storage =
        userHandlesProvider.notebook.workingDir
            .resolve(".intellijPlatform/kotlinNotebook")
            .createDirectories()

    val platformType = productInfo.productCode
    val platformVersion = productInfo.buildNumber

    val pluginRequest = PluginRequest.parse(pluginId)

    val id = pluginRequest.id
    val channel = pluginRequest.channel
    val version =
        pluginRequest.version
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
