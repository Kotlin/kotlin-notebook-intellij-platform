package org.jetbrains.kotlinx.jupyter.intellij.api

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

fun currentProject(): Project? = ProjectManager.getInstance().openProjects.firstOrNull()
