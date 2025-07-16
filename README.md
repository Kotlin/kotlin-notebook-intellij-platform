# IntelliJ Platform for Kotlin Jupyter

[![JetBrains official project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Kotlin experimental stability](https://img.shields.io/badge/project-experimental-kotlin.svg?colorA=555555&colorB=AC29EC&label=&logo=kotlin&logoColor=ffffff&logoWidth=10)](https://kotlinlang.org/docs/components-stability.html)
![GitHub](https://img.shields.io/github/license/Kotlin/kotlin-notebook-intellij-platform?color=blue&label=License)

This repository contains an integration for [Kotlin notebooks](https://github.com/Kotlin/kotlin-jupyter) that provides access to the IntelliJ Platform APIs and plugins directly from your notebooks.

## IntelliJ Platform Integration

Install this integration by running `%use intellij-platform` in your notebook.
After that, your code can use IntelliJ Platform APIs without declaring any additional dependencies.

**Important**: This integration can only run inside the IntelliJ IDE process, not as a standalone.
Enable embedded mode to run it.

### Features

* Access to IntelliJ Platform APIs directly from your notebook
* Load bundled and installed plugins into your notebook
* Run code in the Event Dispatch Thread (EDT) for UI operations
* Automatic cleanup of resources when the kernel is restarted

### Loading Plugins

You can load bundled plugins from the IDE by their IDs:

```kotlin
loadBundledPlugins("intellij.jupyter", "org.jetbrains.kotlin")
```

Or load installed plugins from your IDE:

```kotlin
loadPlugins("com.github.b3er.idea.plugins.arc.browser")
```

### Running Code in EDT

For UI operations, you need to run code in the Event Dispatch Thread:

```kotlin
runInEdt {
    // Your UI code here
}
```

### Minimum IDE Version Requirements

The full integration experience requires IntelliJ IDE version 2025.2 or higher. Some features like `notebookDisposable` and `loadPlugins()` may not be fully supported in earlier versions.

## Example Usage

Here's a simple example of using the IntelliJ Platform integration:

```
%use intellij-platform
```

```kotlin
// Load the Kotlin plugin
loadPlugins("org.jetbrains.kotlin")
```

```kotlin
// Access IntelliJ Platform APIs
import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationManager

// Get the current project
val project = currentProjectFromNotebook(notebook)

// Run UI operations in EDT
runInEdt {
    // Your UI code here
    println("Current project: ${project?.name}")
}
```

## Development Status

This integration is currently in an experimental state. APIs may change in future releases.

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).