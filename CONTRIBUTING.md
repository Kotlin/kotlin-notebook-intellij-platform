# Contributing to the Kotlin Notebook IntelliJ Platform Integration

The current local development process involves a few manual steps:

1. Create a `Local.json` file with the following content:
    ```json
    {
      "description": "IntelliJ Platform that can be used in embedded mode of Kotlin Notebook",
      "properties": [
        { "name": "v", "value": "0.0.2-%VERSION%-SNAPSHOT" },
        { "name": "v-renovate-hint", "value": "update: package=org.jetbrains.kotlinx:kotlin-jupyter-intellij-platform" }
      ],
      "link": "https://plugins.jetbrains.com/docs/intellij/welcome.html",
      "repositories": [
        "https://packages.jetbrains.team/maven/p/kds/kotlin-ds-maven"
      ],
      "dependencies": [
        "org.jetbrains.kotlinx:kotlin-jupyter-intellij-platform:$v"
      ]
    }
    ```
2. Edit `gradle.properties` and increase the `devAddition` property from `1` to `2`.
3. Update the `Local.json` file and replace `%VERSION%` with the same version number used in the previous step, for example changing `2` to get "0.0.2-2-SNAPSHOT".
4. Run the `publishToMavenLocal` task to publish the integration locally.
5. Load the local integration artifact using `%use /path/to/Local.json` instead of `%use intellij-platform`.
6. Reload the Kotlin Notebook Kernel.

To publish a newer version of the local artifact, repeat steps 2-6.
Note that updating the version number is necessary to invalidate the dependency cache.
