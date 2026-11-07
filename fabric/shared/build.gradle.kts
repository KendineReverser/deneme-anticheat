plugins {
    `java-library`
    deneme.`base-conventions`
}

repositories {
    exclusive("https://maven.fabricmc.net/") {
        includeGroup("net.fabricmc")
        includeGroup("net.fabricmc.fabric-api")
    }
    exclusive("https://jitpack.io", { mavenContent { releasesOnly() } }) {
        includeGroup("com.github.Fallen-Breath.conditional-mixin")
    }
    exclusive("https://nexus.scarsz.me/content/repositories/releases", { mavenContent { releasesOnly() } }) {
        includeGroup("github.scarsz")
    }
    mavenCentral()
}

dependencies {
    compileOnly(project(":common"))
    compileOnly(project(":api"))
    compileOnly(project(":api-internal"))
    compileOnly(project(":api-shims"))

    compileOnly(libs.netty)
    compileOnly(files(rootProject.file("libs/packetevents-api.jar")))

    compileOnly(libs.fabric.loader)

    compileOnly(libs.cloud.fabric)
    compileOnly(libs.luckperms)

    compileOnly("org.yaml:snakeyaml:2.2")
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")
    compileOnly(libs.jetbrains.annotations)
}
