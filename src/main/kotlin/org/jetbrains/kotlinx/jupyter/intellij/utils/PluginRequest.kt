// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlinx.jupyter.intellij.utils

import org.jetbrains.kotlinx.jupyter.intellij.pluginRepository

private const val DEFAULT_CHANNEL = "default"

internal data class PluginRequest(val id: String, var version: String? = null, val channel: String = DEFAULT_CHANNEL) {
    companion object {
        /**
         * The [notation] can be specified in one of the following formats:
         * - `pluginId`
         * - `pluginId@channel`
         * - `pluginId:version`
         * - `pluginId:version@channel`
         *
         * @param notation The plugin notation to parse.
         * @return The [PluginRequest] object with the parsed information.
         */
        fun parse(notation: String): PluginRequest {
            val (idWithVersion, channel) =
                notation.split("@", limit = 2).let {
                    it.first() to (it.getOrNull(1) ?: DEFAULT_CHANNEL)
                }

            val (id, version) =
                idWithVersion.split(":", limit = 2).let {
                    it.first() to it.getOrNull(1)
                }

            return PluginRequest(id, version, channel)
        }
    }
}

internal fun PluginRequest.resolveCompatibleVersion(
    platformType: String,
    platformVersion: String,
) = pluginRepository.pluginManager.searchCompatibleUpdates(
    build = "$platformType-$platformVersion",
    xmlIds = listOf(id),
    channel = channel,
).firstOrNull()?.version
