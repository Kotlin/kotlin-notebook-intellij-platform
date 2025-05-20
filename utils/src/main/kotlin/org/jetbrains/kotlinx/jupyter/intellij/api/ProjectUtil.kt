package org.jetbrains.kotlinx.jupyter.intellij.api

import com.intellij.openapi.project.ProjectManager

fun currentProject() = ProjectManager.getInstance().openProjects.firstOrNull()
