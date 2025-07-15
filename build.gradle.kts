import com.github.jengelman.gradle.plugins.shadow.transformers.ComponentsXmlResourceTransformer
import org.jetbrains.gradle.shadow.registerShadowJarTasksBy
import org.jetbrains.kotlinx.publisher.apache2
import org.jetbrains.kotlinx.publisher.composeOfTaskOutputs
import org.jetbrains.kotlinx.publisher.developer

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jupyter.api)
    alias(libs.plugins.publisher)
    alias(libs.plugins.shadowJar.util)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.intellij.platform.base)
    alias(libs.plugins.ben.manes.versions)
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

val shadowJar: Configuration by configurations.creating

dependencies {
    api(libs.kotlin.jupyter.lib)
    api(libs.dataframe.core)
    api(libs.dataframe.jupyter)
    implementation(libs.intellij.structure.ide)
    implementation(libs.intellij.pluginRepositoryRestClient)
    testImplementation(kotlin("test"))

    intellijPlatform {
        intellijIdeaCommunity(libs.versions.intellijPlatform, useInstaller = false)
        bundledPlugin("intellij.jupyter")
    }

    shadowJar.apply {
        this(rootProject)
        exclude(group = "org.jetbrains.kotlin")
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

val platformShadowJar =
    tasks.registerShadowJarTasksBy(
        shadowJar,
        withSources = true,
        binaryTaskConfigurator = {
            mergeServiceFiles()
            exclude("**/module-info.class")
            exclude("org/jetbrains/kotlinx/dataframe/**")
            transform(ComponentsXmlResourceTransformer())
            manifest {
                attributes["Implementation-Version"] = project.version
            }
        },
    )

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
        composeOfTaskOutputs(platformShadowJar)
    }
}
