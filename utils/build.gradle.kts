plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publisher)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.intellij.platform.base)
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(libs.versions.intellijPlatform)
    }
}

kotlinPublications {
    publication {
        publicationName.set("kotlin-jupyter-intellij-platform-utils")
        description.set("Utility methods for Kotlin Jupyter kernel integration for the IntelliJ Platform")
    }
}
