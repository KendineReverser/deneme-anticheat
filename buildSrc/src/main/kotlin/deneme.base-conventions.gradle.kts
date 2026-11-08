import versioning.BuildConfig

plugins {
    `java-library`
    id("io.freefair.lombok")
}

group = rootProject.group
version = rootProject.version
description = rootProject.description

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    disableAutoTargetJvm()
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    processResources {
        val properties = mapOf(
            "timestamp" to rootProject.ext["timestamp"],
            "version" to project.version.toString(),
            "git_commit" to rootProject.ext["git_commit"],
            "git_branch" to rootProject.ext["git_branch"],
            "git_repo" to rootProject.ext["git_repo"],
            "git_org" to rootProject.ext["git_org"],
            "build_shade_pe" to BuildConfig.shadePE,
            "build_relocate" to BuildConfig.relocate,
            "build_release" to BuildConfig.release,
        )

        properties.forEach { (key, value) -> inputs.property(key, value) }

        filesMatching(listOf("fabric.mod.json", "denemeanticheat.properties")) {
            expand(properties)
        }
    }
}

defaultTasks("build")
