package org.jetbrains.kotlinx.jupyter.intellij.util

import java.io.File
import java.util.jar.JarFile

fun findPackagesInJarOrDirectory(
    path: String,
    packages: SetBuilder<String>,
) {
    val file = File(path)

    if (file.isDirectory) {
        // findPackagesInDirectory(file, file, packages)
    } else if (file.isFile && file.extension == "jar") {
        findPackagesInJar(file, packages)
    }
}

@Suppress("unused")
fun findPackagesInDirectory(
    root: File,
    current: File,
    packages: SetBuilder<String>,
) {
    current.listFiles()?.forEach { file ->
        if (file.isDirectory) {
            findPackagesInDirectory(root, file, packages)
        } else if (file.extension == "class") {
            val relativePath = root.toURI().relativize(file.toURI()).path
            val packageName = convertPathToPackage(relativePath)
            packages.add(packageName)
        }
    }
}

private fun findPackagesInJar(
    file: File,
    packages: SetBuilder<String>,
) {
    JarFile(file).use { jarFile ->
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryName = entry.name
            if (entryName.endsWith(".class")) {
                val packageName = convertPathToPackage(entryName)
                packages.add(packageName)
            }
        }
    }
}

private fun convertPathToPackage(relativePath: String) =
    relativePath
        .substring(0, relativePath.lastIndexOf('/'))
        .replace('/', '.')
