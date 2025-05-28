package org.jetbrains.kotlinx.jupyter.intellij.api

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager

fun currentEditor(): FileEditor? = currentProject()?.let { project ->
    FileEditorManager.getInstance(project).selectedEditor
}
