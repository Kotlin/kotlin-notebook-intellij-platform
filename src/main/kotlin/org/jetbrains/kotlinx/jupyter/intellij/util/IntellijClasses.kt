package org.jetbrains.kotlinx.jupyter.intellij.util

object IntellijClasses {
    val PluginClassLoader = getClassForName("com.intellij.ide.plugins.cl.PluginClassLoader")
    val UrlClassLoader = getClassForName("com.intellij.util.lang.UrlClassLoader")
    val ClassPath = getClassForName("com.intellij.util.lang.ClassPath")
}
