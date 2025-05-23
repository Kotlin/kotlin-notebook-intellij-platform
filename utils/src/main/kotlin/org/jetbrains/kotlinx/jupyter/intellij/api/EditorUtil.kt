package org.jetbrains.kotlinx.jupyter.intellij.api

import com.intellij.openapi.fileEditor.FileEditorManager

fun currentEditor() = currentProject()?.let {project ->
    FileEditorManager.getInstance(project).selectedEditor
}
