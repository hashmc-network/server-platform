pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc"
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "server-platform"

include(":common:network")
include(":common:model")

include(":platform:persistence")
include(":platform:messaging")
include(":platform:core")
include(":platform:proxy")

include(":game-engine")

include(":games:bedwars")
include(":games:duels")

include(":servers:lobby")
include(":servers:auth")
