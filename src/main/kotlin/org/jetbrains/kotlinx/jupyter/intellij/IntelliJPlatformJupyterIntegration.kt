package org.jetbrains.kotlinx.jupyter.intellij

import com.jetbrains.plugin.structure.ide.ProductInfoBasedIde
import com.jetbrains.plugin.structure.ide.createIde
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode.SKIP_SILENTLY
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParser
import org.jetbrains.kotlinx.jupyter.api.KotlinKernelHost
import org.jetbrains.kotlinx.jupyter.api.Notebook
import org.jetbrains.kotlinx.jupyter.api.annotations.JupyterLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.JupyterIntegration
import org.jetbrains.kotlinx.jupyter.api.libraries.createLibrary
import org.jetbrains.kotlinx.jupyter.api.libraries.dependencies
import org.jetbrains.kotlinx.jupyter.api.textResult
import org.jetbrains.kotlinx.jupyter.intellij.utils.getIntelliJPlatformPath
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.pathString

private const val ERROR_INCOMPATIBLE_MODE = "IntelliJ Platform integration should be loaded inside the IDE process only"
private const val ERROR_MISSING_PRODUCT_INFO = "Cannot find `product-info.json` file in the IntelliJ Platform installation."

@JupyterLibrary
class IntelliJPlatformJupyterIntegration : JupyterIntegration() {
    override fun Builder.onLoaded() {
        onLoaded {
            onIntegrationLoaded(notebook)
        }
    }

    private fun KotlinKernelHost.onIntegrationLoaded(notebook: Notebook) {
        if (!notebook.kernelRunMode.isRunInsideIntellijProcess) {
            return error(ERROR_INCOMPATIBLE_MODE)
        }

        val idePath = getIntelliJPlatformPath()
        val productInfo = idePath.resolveProductInfo() ?: return error(ERROR_MISSING_PRODUCT_INFO)

        val jars = productInfo.launch
            ?.firstOrNull()
            ?.bootClassPathJarNames
            .orEmpty()
            .map { idePath.resolve("lib/$it") }
            .toSet()

        addLibrary(
            createLibrary(notebook) {
                importPackage<IntelliJPlatformJupyterIntegration>()
                import("org.jetbrains.kotlinx.jupyter.intellij.api.*")

                addDependenciesAndImports(jars)
            },
        )
    }

    private fun Builder.addDependenciesAndImports(pathsToAdd: Set<Path>) {
        dependencies {
            pathsToAdd.forEach {
                implementation(it.pathString)
            }
        }
    }

    private fun Path.resolveProductInfo(): ProductInfo? {
        val file = resolve("Resources/product-info.json").takeIf { it.exists() }
        val parser = ProductInfoParser()

        return file?.let { parser.parse(it )}
    }

    private fun Path.resolveIde(): ProductInfoBasedIde {
        val ide = createIde {
            missingLayoutFileMode = SKIP_SILENTLY
            path = this@resolveIde
        } as? ProductInfoBasedIde
        requireNotNull(ide)

        return ide
    }

    private fun KotlinKernelHost.error(message: String) =
        display(textResult(message), null)
}
