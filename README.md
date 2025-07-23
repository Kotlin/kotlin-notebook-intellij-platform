# Kotlin Notebook IntelliJ Platform Integration

[![JetBrains official project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Kotlin experimental stability](https://img.shields.io/badge/project-experimental-kotlin.svg?colorA=555555&colorB=AC29EC&label=&logo=kotlin&logoColor=ffffff&logoWidth=10)](https://kotlinlang.org/docs/components-stability.html)
![GitHub](https://img.shields.io/github/license/Kotlin/kotlin-notebook-intellij-platform?color=blue&label=License)

This integration enables direct experimentation with IntelliJ Platform APIs within the active IntelliJ IDEA runtime, eliminating traditional plugin development barriers.

## Requirements

IntelliJ IDEA 2025.2 or higher is required for full functionality.

## Quick Start

1. Create a new Kotlin Notebook file (`.ipynb`) using <kbd>⌘⇧N</kbd> (macOS) or <kbd>Ctrl+Shift+N</kbd> (Windows/Linux).
2. **Important**: Switch to **Run in IDE Process** mode in the notebook toolbar.
3. In the first cell, execute: `%use intellij-platform`.

## Basic Example

```kotlin
%use intellij-platform
```

```kotlin
import com.intellij.ui.dsl.builder.panel

panel {
  row {
    checkBox("Enable feature")
      .comment("This checkbox is fully interactive")
  }
}
```

## Documentation

For comprehensive documentation, examples, and API reference, see:
**[IntelliJ Platform Plugin SDK | Kotlin Notebook Integration](https://plugins.jetbrains.com/docs/intellij/tools-kotlin-notebook.html)**

**Examples**: [kotlin-notebook-intellij-platform/examples](https://github.com/Kotlin/kotlin-notebook-intellij-platform/tree/master/examples)

**Support**: [JetBrains Platform Forum - Kotlin Notebook](https://platform.jetbrains.com/c/intellij-platform/kotlin-notebook/25)

## Contributing
Read the [Contributing Guidelines](CONTRIBUTING.md).

## Code of Conduct
This project and the corresponding community are governed by the [JetBrains Open Source and Community Code of Conduct](https://github.com/jetbrains#code-of-conduct). Please make sure you read it.

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).