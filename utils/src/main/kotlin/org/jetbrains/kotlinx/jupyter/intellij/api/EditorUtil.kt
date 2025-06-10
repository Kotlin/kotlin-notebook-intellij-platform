package org.jetbrains.kotlinx.jupyter.intellij.api

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager

/**
 * Returns the current [FileEditor] instance or null if no editor is open.
 *
 * @return the current [FileEditor] instance or null
 */
fun currentEditor(): FileEditor? = currentProject()?.let { project ->
    FileEditorManager.getInstance(project).selectedEditor
}
