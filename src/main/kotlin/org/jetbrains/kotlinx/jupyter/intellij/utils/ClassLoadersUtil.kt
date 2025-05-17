package org.jetbrains.kotlinx.jupyter.intellij.utils

import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import java.net.URLClassLoader
import java.nio.file.Path

fun getClassForName(className: String): Class<*> {
    return Class.forName(className)
}

fun classLoaderSequence() = generateSequence(JupyterIntegration::class.java.classLoader) { it.parent }

fun findAllPaths(
    classLoader: ClassLoader,
    resultSet: MutableSet<String>,
) {
    when {
        classLoader is URLClassLoader -> {
            resultSet.addAll(classLoader.urLs.map { it.path })
        }
        IntellijClasses.UrlClassLoader.isAssignableFrom(classLoader::class.java) -> {
            val classPath =
                invokeMethod<Any>(
                    IntellijClasses.UrlClassLoader.name,
                    "getClassPath",
                    classLoader,
                    argumentsOf(),
                )

            val urls =
                invokeMethod<List<Path>>(
                    IntellijClasses.ClassPath.name,
                    "getBaseUrls",
                    classPath,
                    argumentsOf(),
                )

            resultSet.addAll(urls.map { it.toAbsolutePath().toString() })
        }
    }

    val parentClassLoader = classLoader.parent
    when {
        parentClassLoader != null -> {
            findAllPaths(parentClassLoader, resultSet)
        }
        IntellijClasses.PluginClassLoader.isInstance(classLoader) -> {
            val parents: Array<ClassLoader> =
                invokeMethod(
                    IntellijClasses.PluginClassLoader.name,
                    "getAllParentsClassLoaders",
                    classLoader,
                    argumentsOf(),
                )

            for (parent in parents) {
                findAllPaths(parent, resultSet)
            }
        }
    }
}

fun getAllIntellijPaths(): Set<String> {
    val allClassLoaders = classLoaderSequence()
    val pluginClassLoaders = allClassLoaders.filter { IntellijClasses.PluginClassLoader.isInstance(it) }
    return buildSet {
        for (cl in pluginClassLoaders) {
            findAllPaths(cl, this)
        }
    }
}
