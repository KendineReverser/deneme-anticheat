dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }

        create("testlibs") {
            from(files("testlibs.versions.toml"))
        }
    }
}

pluginManagement {
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "FabricMC"
                    url = uri("https://maven.fabricmc.net/")
                }
            }
            filter {
                includeModule("fabric-loom", "fabric-loom.gradle.plugin")
                includeGroupByRegex("net.fabricmc.*")
            }
        }

        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "deneme-anticheat"
include("api")
include("api-internal")
include("api-shims")
include("common")
include("fabric")
include(":fabric:shared")
include(":fabric:intermediary")
include(":fabric:intermediary:mc1161")
include(":fabric:intermediary:mc1171")
include(":fabric:intermediary:mc1194")
include(":fabric:intermediary:mc1205")
include(":fabric:intermediary:mc12111")
