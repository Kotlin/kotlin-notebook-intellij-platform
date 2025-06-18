package org.jetbrains.kotlinx.jupyter.intellij.utils

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.ide.plugins.cl.ResolveScopeManager
import java.io.IOException
import java.net.URL
import java.util.Collections
import java.util.Enumeration
import kotlin.collections.ArrayDeque

@Suppress("UnstableApiUsage")
class IntelliJPlatformClassloader : ClassLoader() {

    val allParents = mutableListOf<ClassLoader>()

    private fun getParents(classLoader: ClassLoader): List<ClassLoader> {
        return when {
            classLoader is PluginClassLoader -> classLoader.getAllParentsClassLoaders().toList()
            else -> generateSequence(classLoader.parent) { it.parent }.toList()
        }
    }


    fun addParents(parents: List<ClassLoader>) {
        val newParents = parents // collectWithParents(parents, ::getParents)
        val sorted = topologicalMergeSortedAndNew(allParents, newParents, ::getParents)

        allParents.clear()
        allParents.addAll(sorted)
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // First, check if a class is already loaded
        val loaded = findLoadedClass(name)
        if (loaded != null) {
            if (resolve) resolveClass(loaded)
            return loaded
        }

        for (parent in allParents) {
            try {
                // Simplification of logic from PluginClassLoader#tryLoadingClass: further generations will be grateful
                // Ideally, we also need to take UrlClassLoaders into consideration: these guys load plugin libraries
                val clazz = if (parent is PluginClassLoader) {
                    if (parent.calculateConsistency(name) != null) {
                        continue
                    }
                    parent.loadClassInsideSelf(name)
                } else {
                    parent.loadClass(name)
                }
                if (clazz == null) continue
                if (resolve) resolveClass(clazz)
                return clazz
            } catch (_: ClassNotFoundException) {
                // Try next
            }
        }

        // Class isn't found in any parent
        throw ClassNotFoundException("Class $name not found in any delegate classloader")
    }

    // Optional: restrict resources the same way
    override fun getResource(name: String): URL? {
        for (parent in allParents) {
            val resource = parent.getResource(name)
            if (resource != null) return resource
        }
        return null
    }

    override fun getResources(name: String): Enumeration<URL?>? {
        val resources = allParents.flatMap {
            try {
                it.getResources(name).toList()
            } catch (_: IOException) {
                emptyList()
            }
        }
        return Collections.enumeration(resources)
    }

    private val resolveScopeField = PluginClassLoader::class.java.getDeclaredField("_resolveScopeManager").apply {
        isAccessible = true
    }

    /**
     * See [ResolveScopeManager.isDefinitelyAlienClass].
     */
    private fun PluginClassLoader.calculateConsistency(name: String, force: Boolean = false): String? {
        return this.packagePrefix?.let {
            (resolveScopeField.get(this) as ResolveScopeManager)
                .isDefinitelyAlienClass(name = name, packagePrefix = it, force = force)
        }
    }

    /**
     * ClassLoader A is before classloader B in the resulting list => classloader B doesn't have classloader A
     * as a direct or transitive parent.
     */
    private fun <T> topologicalMergeSortedAndNew(
        sorted: List<T>,
        new: List<T>,
        getParents: (T) -> List<T>,
    ): List<T> {
        val allElements = (sorted + new).distinct()
        val dependencyGraph = mutableMapOf<T, MutableList<T>>() // children -> parent
        val inDegree = mutableMapOf<T, Int>().withDefault { 0 }
        // Initialize inDegree and graph
        for (element in allElements) {
            val parents = getParents(element)
            for (parent in parents) {
                if (parent !in allElements) continue // Skip missing parents
                dependencyGraph.getOrPut(element) { mutableListOf() }.add(parent)
                inDegree.compute(parent) { _, v -> (v ?: 0) + 1 }
            }
            inDegree.putIfAbsent(element, 0)
        }
        // Kahn's algorithm
        val queue = ArrayDeque(allElements.filter { inDegree.getValue(it) == 0 })
        val result = mutableListOf<T>()
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            result.add(node)
            for (parent in dependencyGraph[node].orEmpty()) {
                val deg = inDegree.getValue(parent) - 1
                inDegree[parent] = deg
                if (deg == 0) queue.add(parent)
            }
        }
        // Optional: Detect cycles
        if (result.size < allElements.size) {
            error("Cycle detected or unresolved dependencies")
        }
        return result
    }
}
