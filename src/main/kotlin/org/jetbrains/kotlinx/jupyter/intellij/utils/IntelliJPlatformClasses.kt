// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlinx.jupyter.intellij.utils

/**
 * A part of the internal API for resolving classes from the currently run IntelliJ Platform.
 */
object IntelliJPlatformClasses {
    val PathManager = getClassForName("com.intellij.openapi.application.PathManager")
}
