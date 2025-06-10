package org.jetbrains.kotlinx.jupyter.intellij.api

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

/**
 * Returns the current open [Project] instance or null if no projects are open.
 *
 * @return the current [Project] instance or null
 */
fun currentProject(): Project? = ProjectManager.getInstance().openProjects.firstOrNull()
