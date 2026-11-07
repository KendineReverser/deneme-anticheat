import versioning.BuildConfig

@Suppress("PropertyName")
val minecraft_version: String by project

plugins {
    `maven-publish`
    alias(libs.plugins.fabric.loom.unobfuscated)
    deneme.`base-conventions`
    deneme.`jij-conventions`
}

repositories {
    maven {
        name = "bundled"
        url = uri(rootProject.file("libs/maven"))
    }

    exclusive("https://maven.fabricmc.net/") {
        includeGroup("net.fabricmc")
        includeGroup("net.fabricmc.fabric-api")
    }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")

    include(project(":fabric:shared"))
    include(project(":fabric:intermediary"))
    include("ac.deneme:packetevents-fabric:2.14.1")
}

publishing.publications.create<MavenPublication>("maven") {
    artifact(tasks["jar"])
}

tasks {
    jar {
        archiveBaseName = "${rootProject.name}-fabric"
        archiveVersion = rootProject.version as String
    }
}
