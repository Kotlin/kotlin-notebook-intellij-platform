package org.jetbrains.kotlinx.jupyter.intellij.utils

/**
 * A part of the internal API for resolving classes from the currently run IntelliJ Platform.
 */
object IntelliJPlatformClasses {
    val PathManager = getClassForName("com.intellij.openapi.application.PathManager")
}
