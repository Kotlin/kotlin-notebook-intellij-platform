package org.jetbrains.kotlinx.jupyter.intellij.utils.devMode

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.util.lang.UrlClassLoader
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import java.net.URLClassLoader
import java.nio.file.Path

/**
 * Uses alternative approach for getting all JARs: traverses class loader hierarchy instead of
 * reading product info JSON (that might be absent in dev mode)
 */
@Suppress("UnstableApiUsage")
fun getAllIntellijPathsForDevMode(): Set<Path> {
    val allClassLoaders = classLoaderSequence()
    val pluginClassLoaders = allClassLoaders.filter { it is PluginClassLoader }
    return buildSet {
        for (cl in pluginClassLoaders) {
            findAllPaths(cl, this)
        }
    }
}

private fun classLoaderSequence() = generateSequence(JupyterIntegration::class.java.classLoader) { it.parent }

@Suppress("UnstableApiUsage")
private fun findAllPaths(
    classLoader: ClassLoader,
    resultSet: MutableSet<Path>,
) {
    when (classLoader) {
        is URLClassLoader -> {
            resultSet.addAll(classLoader.urLs.map { Path.of(it.path) })
        }
        is UrlClassLoader -> {
            val classPath = classLoader.classPath
            val urls = classPath.baseUrls
            resultSet.addAll(urls.map { it.toAbsolutePath() })
        }
    }

    val parentClassLoader = classLoader.parent
    when {
        parentClassLoader != null -> {
            findAllPaths(parentClassLoader, resultSet)
        }
        classLoader is PluginClassLoader -> {
            val parents: Array<ClassLoader> = classLoader.getAllParentsClassLoaders()

            for (parent in parents) {
                findAllPaths(parent, resultSet)
            }
        }
    }
}
