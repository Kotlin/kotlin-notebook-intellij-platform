package org.jetbrains.kotlinx.jupyter.intellij.api

import com.intellij.ide.plugins.PluginManagerCore
import jupyter.kotlin.EXECUTE
import jupyter.kotlin.ScriptTemplateWithDisplayHelpers
import jupyter.kotlin.USE
import org.jetbrains.kotlinx.dataframe.AnyFrame
import org.jetbrains.kotlinx.dataframe.api.toDataFrame

/**
 * Prints the list of installed plugins in the currently run IDE.
 */
@Suppress("unused")
fun ScriptTemplateWithDisplayHelpers.printPlugins(): AnyFrame {
    USE {
        EXECUTE("%use dataframe")
    }

    return with(PluginManagerCore.plugins) {
        mapOf(
            "ID" to map { it.pluginId },
            "name" to map { it.name },
            "version" to map { it.version },
            "vendor" to map { it.vendor },
            "bundled" to map { it.isBundled },
            "loaded" to map { PluginManagerCore.isLoaded(it.pluginId) },
            "enabled" to map { !PluginManagerCore.isDisabled(it.pluginId) },
        )
    }.toDataFrame()
}