package org.jetbrains.kotlinx.jupyter.intellij.api

import jupyter.kotlin.ScriptTemplateWithDisplayHelpers
import org.jetbrains.kotlinx.jupyter.api.outputs.display
import com.intellij.openapi.application.runInEdt as runInEdtBase

/**
 * Runs the given [block] in the EDT.
 * If an exception is thrown, it is displayed in the output.
 */
@Suppress("unused")
fun ScriptTemplateWithDisplayHelpers.runInEdt(block: () -> Unit) = runInEdtBase {
    runCatching {
        block()
    }.onFailure {
        userHandlesProvider.notebook.display(it, null)
    }
}
