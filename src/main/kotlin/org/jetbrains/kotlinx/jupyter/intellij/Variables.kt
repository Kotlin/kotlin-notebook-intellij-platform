package org.jetbrains.kotlinx.jupyter.intellij

import com.intellij.ide.plugins.PluginMainDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.platform.plugins.parser.impl.PluginDescriptorBuilder
import com.jetbrains.plugin.structure.ide.ProductInfoBasedIde
import com.jetbrains.plugin.structure.ide.createIde
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode.SKIP_SILENTLY
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParser
import jupyter.kotlin.ScriptTemplateWithDisplayHelpers
import org.jetbrains.intellij.pluginRepository.PluginRepository
import org.jetbrains.intellij.pluginRepository.PluginRepositoryFactory
import org.jetbrains.kotlinx.jupyter.intellij.utils.getIntelliJPlatformPath
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Represents a disposable used for managing the IntelliJ Platform lifetime of the current notebook.
 */
val notebookDisposable: Disposable = Disposer.newCheckedDisposable("Kotlin Notebook")

/**
 * Represents a plugin descriptor for the plugin created with Kotlin Notebook IntelliJ Platform integration.
 */
@Suppress("UnstableApiUsage")
val ScriptTemplateWithDisplayHelpers.notebookPluginDescriptor: PluginMainDescriptor
    get() = PluginMainDescriptor(
        raw = PluginDescriptorBuilder.builder().apply {
            id = "kotlin.notebook.plugin"
            name = "Kotlin Notebook Plugin"
            version = "1.0"
        }.build(),
        pluginPath = notebook.workingDir,
        isBundled = false,
    ).apply {
        pluginClassLoader = notebook.intermediateClassLoader
    }

/**
 * Represents the resolved file system path to the IntelliJ Platform installation directory.
 */
val idePath: Path by lazy {
    getIntelliJPlatformPath()
}

/**
 * Represents an IDE instance, which allows interacting with plugins and resolve their dependencies based on the IDE's configuration.
 */
val ide: ProductInfoBasedIde by lazy {
    requireNotNull(
        createIde {
            missingLayoutFileMode = SKIP_SILENTLY
            path = idePath
        } as? ProductInfoBasedIde,
    )
}

/**
 * Provides a lazily initialized instance of [PluginRepository], which is responsible for
 * managing plugins via the JetBrains Marketplace plugin repository.
 */
val pluginRepository: PluginRepository by lazy {
    PluginRepositoryFactory.create("https://plugins.jetbrains.com")
}

/**
 * Provides a lazily initialized instance of the [PluginManager], which allows accessing information about plugins
 * and their states in the current IDE environment.
 */
val pluginManager: PluginManager by lazy {
    PluginManager.getInstance()
}

/**
 * Lazily initialized property containing information about the current IntelliJ Platform product.
 */
val productInfo: ProductInfo by lazy {
    val parser = ProductInfoParser()
    val file = listOf(
        idePath.resolve("product-info.json"),
        idePath.resolve("Resources/product-info.json"),
    ).firstOrNull { it.exists() } ?: error("The product-info.json file not found")
    requireNotNull(parser.parse(file))
}
