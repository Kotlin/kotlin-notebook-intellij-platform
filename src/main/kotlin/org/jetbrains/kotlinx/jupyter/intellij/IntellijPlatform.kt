@file:Suppress("unused")

package org.jetbrains.kotlinx.jupyter.intellij

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import jupyter.kotlin.ScriptTemplateWithDisplayHelpers
import org.jetbrains.kotlinx.jupyter.api.Notebook
import org.jetbrains.kotlinx.jupyter.intellij.utils.IntellijDataProviderProxy
import org.jetbrains.kotlinx.jupyter.intellij.utils.createProxy
import org.jetbrains.kotlinx.jupyter.intellij.utils.getPropertyValue

/**
 * Returns the current open [Project] instance or null if no projects are open.
 *
 * @return the current [Project] instance or null
 */
fun ScriptTemplateWithDisplayHelpers.currentProject(): Project? {
    return currentProjectFromNotebook(notebook)
}

/**
 * Returns the current open [Project] instance or null if no projects are open.
 */
internal fun currentProjectFromNotebook(notebook: Notebook): Project? {
    return notebook.intellijDataProvider?.currentProject ?: currentProjectFromFocus()
}

/**
 * Returns the [IntellijDataProviderProxy] instance for the current notebook, if any.
 */
val Notebook.intellijDataProvider: IntellijDataProviderProxy? get() {
    val providerInstance = kernelRunMode
        .getPropertyValue("intellijDataProvider") ?: return null

    return createProxy(providerInstance)
}

/**
 * Returns the current open [Project] instance or null if no projects are open.
 */
private fun currentProjectFromFocus(): Project? {
    return DataManager.getInstance()
        .dataContextFromFocusAsync
        .blockingGet(3000)
        ?.let(CommonDataKeys.PROJECT::getData)
}

/**
 * Returns the current [FileEditor] instance or null if no editor is open.
 *
 * @return the current [FileEditor] instance or null
 */
fun ScriptTemplateWithDisplayHelpers.currentEditor(): FileEditor? =
    currentProject()?.let { project ->
        FileEditorManager.getInstance(project).selectedEditor
    }

/**
 * Registers the given [instance] as an extension for the given [extensionPointName].
 */
inline fun <reified T : Any> registerExtension(
    extensionPointName: ExtensionPointName<T>,
    instance: T,
) = ApplicationManager.getApplication()
    .extensionArea
    .getExtensionPoint(extensionPointName)
    .registerExtension(instance, notebookDisposable)
