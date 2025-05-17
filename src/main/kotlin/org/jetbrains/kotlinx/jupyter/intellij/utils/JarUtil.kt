package org.jetbrains.kotlinx.jupyter.intellij.utils

import java.io.File
import java.util.jar.JarFile

fun interface ClassifierNamePredicate {
    fun test(classifierName: String): Boolean
}

/**
 * Traverses given [path] as a directory or as a JAR file,
 * adds found JVM packages to [packages].
 * Ignores packages that contain classifiers with names for which [dontAddPackageIfItContainsClassifier] returns true.
 */
fun findPackagesInJarOrDirectory(
    path: String,
    packages: SetBuilder<String>,
    dontAddPackageIfItContainsClassifier: ClassifierNamePredicate = ClassifierNamePredicate { false },
) {
    val file = File(path)
    val ignoredPackages =
        mutableSetOf(
            // ignore root package
            "",
        )

    if (file.isDirectory) {
        // findPackagesInDirectory(file, file, packages, ignoredPackages, dontAddPackageIfItContainsClassifier)
    } else if (file.isFile && file.extension == "jar") {
        findPackagesInJar(file, packages, ignoredPackages, dontAddPackageIfItContainsClassifier)
    }
}

@Suppress("unused")
fun findPackagesInDirectory(
    root: File,
    current: File,
    packages: SetBuilder<String>,
    ignoredPackages: MutableSet<String>,
    dontAddPackageIfItContainsClassifier: ClassifierNamePredicate,
) {
    current.listFiles()?.forEach { file ->
        if (file.isDirectory) {
            findPackagesInDirectory(root, file, packages, ignoredPackages, dontAddPackageIfItContainsClassifier)
        } else if (file.extension == "class") {
            val relativePath = root.toURI().relativize(file.toURI()).path
            val classifierName = file.nameWithoutExtension
            val packageName = convertPathToPackage(relativePath)

            processPackageAndClassifier(
                packages,
                ignoredPackages,
                packageName,
                dontAddPackageIfItContainsClassifier,
                classifierName,
            )
        }
    }
}

private fun findPackagesInJar(
    file: File,
    packages: SetBuilder<String>,
    ignoredPackages: MutableSet<String>,
    dontAddPackageIfItContainsClassifier: ClassifierNamePredicate,
) {
    JarFile(file).use { jarFile ->
        val entries = jarFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryName = entry.name
            if (entryName.endsWith(".class")) {
                val classifierName = entryName.substringBeforeLast(".class")
                val packageName = convertPathToPackage(entryName)
                processPackageAndClassifier(
                    packages,
                    ignoredPackages,
                    packageName,
                    dontAddPackageIfItContainsClassifier,
                    classifierName,
                )
            }
        }
    }
}

private fun processPackageAndClassifier(
    packages: SetBuilder<String>,
    ignoredPackages: MutableSet<String>,
    packageName: String,
    dontAddPackageIfItContainsClassifier: ClassifierNamePredicate,
    classifierName: String,
) {
    if (dontAddPackageIfItContainsClassifier.test(classifierName)) {
        ignoredPackages.add(packageName)
        return
    }

    if (ignoredPackages.contains(packageName)) {
        return
    }
    packages.add(packageName)
}

private fun convertPathToPackage(relativePath: String): String {
    val lastSlashIndex = relativePath.lastIndexOf('/')
    if (lastSlashIndex == -1) return ""
    return relativePath
        .substring(0, lastSlashIndex)
        .replace('/', '.')
}
