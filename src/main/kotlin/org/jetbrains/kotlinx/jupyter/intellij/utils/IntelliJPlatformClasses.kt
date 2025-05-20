package org.jetbrains.kotlinx.jupyter.intellij.utils

object IntelliJPlatformClasses {
    val PathManager = getClassForName("com.intellij.openapi.application.PathManager")
}
