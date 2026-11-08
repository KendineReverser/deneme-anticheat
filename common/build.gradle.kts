plugins {
    deneme.`base-conventions`
}

repositories {
    exclusive("https://repo.viaversion.com", { mavenContent { releasesOnly() } }) {
        includeGroup("com.viaversion")
    }

    exclusive("https://nexus.scarsz.me/content/repositories/releases", { mavenContent { releasesOnly() } }) {
        includeGroup("github.scarsz")
    }

    exclusive("https://repo.opencollab.dev/maven-releases/", { mavenContent { releasesOnly() } }) {
        includeGroup("org.geysermc.api")
    }

    exclusive("https://repo.opencollab.dev/maven-snapshots/", { mavenContent { snapshotsOnly() } }) {
        includeGroup("org.geysermc.floodgate")
        includeGroup("org.geysermc.cumulus")
        includeModule("org.geysermc", "common")
        includeModule("org.geysermc", "geyser-parent")
    }

    mavenCentral()
}

dependencies {
    compileOnly(files(rootProject.file("libs/packetevents-api.jar")))

    api(project(":api"))
    api(project(":api-internal"))
    compileOnly(project(":api-shims"))

    api(libs.bundles.cloud.api)
    api(libs.configuralize) {
        artifact {
            classifier = "slim"
        }
        exclude(group = "org.yaml", module = "snakeyaml")
    }
    api(libs.snakeyaml)
    api(libs.fastutil)
    api(libs.adventure.text.minimessage)
    api(libs.hikaricp)

    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.mongoDriverSync)
    compileOnly(libs.netty)
    compileOnly(libs.luckperms)
    compileOnly(libs.viaversion)
    compileOnly(libs.viabackwards)

    compileOnly(libs.geyser.base.api) {
        isTransitive = false
    }

    compileOnly(variantOf(libs.floodgate.api) { classifier("shaded") }) {
        isTransitive = false
    }

    runtimeOnly(libs.slf4j.jdk14)

    testImplementation(libs.junit.jupiter)
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation(libs.sqlite.jdbc)
    testRuntimeOnly(files(rootProject.file("libs/packetevents-api.jar")))
    testRuntimeOnly(project(":api-shims"))
    testRuntimeOnly("com.google.guava:guava:31.0.1-jre")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("DenemePlatformOverride", "fabric")
}
