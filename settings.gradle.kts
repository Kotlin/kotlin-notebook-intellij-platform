plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "ktnb-intellij-platform"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("utils")
