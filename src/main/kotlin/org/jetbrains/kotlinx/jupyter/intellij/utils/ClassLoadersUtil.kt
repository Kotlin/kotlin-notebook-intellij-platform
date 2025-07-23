// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlinx.jupyter.intellij.utils

import java.nio.file.Path
import java.nio.file.Paths

fun getClassForName(className: String): Class<*> {
    return Class.forName(className)
}

/**
 * Gets the path to the current IntelliJ Platform directory.
 * @return Path object representing the IDE installation directory
 */
fun getIntelliJPlatformPath(): Path {
    val homePath =
        invokeMethod<String>(
            IntelliJPlatformClasses.PathManager.name,
            "getHomePath",
            null,
            emptyList(),
        )
    return Paths.get(homePath)
}
