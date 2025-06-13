@file:Suppress("unused")

package org.jetbrains.kotlinx.jupyter.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

/**
 * Returns the current open [Project] instance or null if no projects are open.
 *
 * @return the current [Project] instance or null
 */
fun currentProject(): Project? = ProjectManager.getInstance().openProjects.firstOrNull()

/**
 * Returns the current [FileEditor] instance or null if no editor is open.
 *
 * @return the current [FileEditor] instance or null
 */
fun currentEditor(): FileEditor? = currentProject()?.let { project ->
    FileEditorManager.getInstance(project).selectedEditor
}

/**
 * Registers the given [instance] as an extension for the given [extensionPointName].
 */
inline fun <reified T : Any> registerExtension(extensionPointName: ExtensionPointName<T>, instance: T) =
    ApplicationManager.getApplication()
        .extensionArea
        .getExtensionPoint(extensionPointName)
        .registerExtension(instance, notebookDisposable)
