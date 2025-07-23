// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlinx.jupyter.intellij.utils

import com.intellij.openapi.project.Project

/**
 * Proxy interface for `com.intellij.kotlin.jupyter.core.jupyter.kernel.server.embedded.IntellijDataProvider`
 * Provides some information about the environment in which currently running notebook session lives.
 */
interface IntellijDataProviderProxy {
    val currentProject: Project?
}
