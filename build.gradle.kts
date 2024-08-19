import org.jetbrains.kotlinx.publisher.apache2
import org.jetbrains.kotlinx.publisher.developer

plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jupyter.api)
    alias(libs.plugins.publisher)
    alias(libs.plugins.ktlint)
}

val spaceUsername: String by properties
val spaceToken: String by properties

group = "org.jetbrains.kotlinx"
version = detectVersion()

private fun detectVersion(): String {
    val buildCounter = project.findProperty("build.counter")
    if (buildCounter != null) {
        return buildCounter.toString()
    } else {
        val baseVersion = project.property("baseVersion").toString()
        val devAddition = project.property("devAddition").toString()
        return "$baseVersion-$devAddition-SNAPSHOT"
    }
}

kotlinJupyter {
    addScannerDependency()
    addApiDependency()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
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
        }
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
        publicationName.set("kotlin-jupyter-intellij")
        description.set("Kotlin Jupyter kernel integration for IntelliJ SDK")
    }
}
