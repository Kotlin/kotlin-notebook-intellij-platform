package org.jetbrains.kotlinx.jupyter.intellij.utils

import com.intellij.openapi.project.Project

/**
 * Proxy interface for `com.intellij.kotlin.jupyter.core.jupyter.kernel.server.embedded.IntellijDataProvider`
 * Provides some information about the environment in which currently running notebook session lives.
 */
interface IntellijDataProviderProxy {
    val currentProject: Project?
}
