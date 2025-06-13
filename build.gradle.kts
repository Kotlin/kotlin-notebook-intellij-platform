import org.jetbrains.kotlinx.publisher.apache2
import org.jetbrains.kotlinx.publisher.developer

plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jupyter.api)
    alias(libs.plugins.publisher)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.intellij.platform.base)
}

val spaceUsername: String by properties
val spaceToken: String by properties

group = "org.jetbrains.kotlinx"
version =
    detectVersion().also { version ->
        Logging.getLogger(this::class.java).warn("Detected version: $version")
    }

allprojects {
    version = rootProject.version
}

private fun detectVersion(): String {
    val buildNumber = project.findProperty("build.number")
    if (buildNumber != null) {
        return buildNumber.toString()
    } else {
        val baseVersion = project.property("baseVersion").toString()
        val devAddition = project.property("devAddition").toString()
        return "$baseVersion-$devAddition-SNAPSHOT"
    }
}

kotlinJupyter {
    addApiDependency()
}

tasks.processJupyterApiResources {
    libraryProducers = listOf("org.jetbrains.kotlinx.jupyter.intellij.IntelliJPlatformJupyterIntegration")
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    api(libs.kotlin.jupyter.lib)
    api(libs.dataframe.core)
    implementation(libs.intellij.structure.ide)
    implementation(libs.intellij.pluginRepositoryRestClient)
    testImplementation(kotlin("test"))

    intellijPlatform {
        intellijIdeaCommunity(libs.versions.intellijPlatform, useInstaller = false)
        bundledPlugin("intellij.jupyter")
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

kotlinPublications {
    defaultGroup.set(group.toString())
    fairDokkaJars.set(false)

    pom {
        inceptionYear.set("2024")
        licenses {
            apache2()
        }
        developers {
            developer("ileasile", "Ilya Muradyan", "Ilya.Muradyan@jetbrains.com")
            developer("hsz", "Jakub Chrzanowski", "Jakub.Chrzanowski@jetbrains.com")
        }
    }

    localRepositories {
        localMavenRepository(project.layout.buildDirectory.dir("maven"))
    }

    remoteRepositories {
        maven {
            name = "kotlin-ds-maven"
            url = uri("https://packages.jetbrains.team/maven/p/kds/kotlin-ds-maven")
            credentials {
                username = spaceUsername
                password = spaceToken
            }
        }
    }

    publication {
        publicationName.set("kotlin-jupyter-intellij-platform")
        description.set("Kotlin Jupyter kernel integration for the IntelliJ Platform")
    }
}
