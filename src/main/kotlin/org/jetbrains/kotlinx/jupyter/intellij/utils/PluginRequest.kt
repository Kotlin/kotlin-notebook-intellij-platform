package org.jetbrains.kotlinx.jupyter.intellij.utils

import org.jetbrains.kotlinx.jupyter.intellij.pluginRepository

private const val DEFAULT_CHANNEL = "default"

internal data class PluginRequest(val id: String, var version: String? = null, val channel: String = DEFAULT_CHANNEL) {
    companion object {
        fun parse(request: String): PluginRequest {
            val (idWithVersion, channel) = request.split("@", limit = 2).let {
                it.first() to (it.getOrNull(1) ?: DEFAULT_CHANNEL)
            }

            val (id, version) = idWithVersion.split(":", limit = 2).let {
                it.first() to it.getOrNull(1)
            }

            return PluginRequest(id, version, channel)
        }
    }
}

internal fun PluginRequest.resolveCompatibleVersion(platformType: String, platformVersion: String) =
    pluginRepository.pluginManager.searchCompatibleUpdates(
        build = "$platformType-$platformVersion",
        xmlIds = listOf(id),
        channel = channel,
    ).firstOrNull()?.version