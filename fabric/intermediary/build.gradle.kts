import versioning.BuildConfig

@Suppress("PropertyName")
val minecraft_version: String by project
@Suppress("PropertyName")
val yarn_mappings: String by project
@Suppress("PropertyName")
val fabric_version: String by project

plugins {
    `maven-publish`
    // No version: loom is already on the classpath from the parent :fabric project,
    // so a version request here would fail compatibility checking.
    id("net.fabricmc.fabric-loom-remap")
    deneme.`base-conventions`
    deneme.`jij-conventions`
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    mappings(loom.officialMojangMappings())
    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", fabric_version))

    modCompileOnly("me.lucko:fabric-permissions-api:0.3.1")

    modImplementation(libs.cloud.fabric)
    modImplementation(libs.fabric.loader)

    // NMS-free Fabric platform code shared with fabric-official lives here.
    implementation(project(":fabric:shared"))
    // PE is JiJ'd once at the fabric/ aggregator; compileOnly here avoids re-nesting it.
    // Use the API artifact so intermediary Loom does not inspect PE's official-mapped nested mods.
    compileOnly(files(rootProject.file("libs/packetevents-api.jar")))
    compileOnly(files(rootProject.file("libs/packetevents-fabric-common.jar")))
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    compileOnly("org.apache.logging.log4j:log4j-api:2.24.3")
}

// The configurations below will only apply to :fabric and its submodules, not its siblings or the root project
allprojects {
    apply(plugin = "net.fabricmc.fabric-loom-remap")
    apply(plugin = "deneme.base-conventions")
    apply(plugin = "maven-publish")


    repositories {
        // 1. Fallback for non-exclusive deps
        if (BuildConfig.mavenLocalOverride) mavenLocal()

        // 2. Exclusive Repositories
        exclusive("https://maven.fabricmc.net/") {
            includeGroup("net.fabricmc")
            includeGroup("net.fabricmc.fabric-api")
        }

        exclusive("https://jitpack.io", { mavenContent { releasesOnly() } }) {
            includeGroup("com.github.Fallen-Breath.conditional-mixin")
        }

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

        exclusive(mavenCentral()) { includeGroup("me.lucko") }

        mavenCentral()
    }

    loom {
        accessWidenerPath = file("src/main/resources/denemeanticheat.accesswidener")
    }

    dependencies {
        // I hate this syntax, is there an alternative to make modCompileOnly(libs.package.name) work?
        val libsx = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        // Use the libs extension from the root project
        modImplementation(libsx.findLibrary("cloud-fabric").get()) {
            exclude(group = "net.fabricmc.fabric-api")
        }
        modImplementation(libsx.findLibrary("fabric-loader").get())

        implementation(project(":common"))
    }

    publishing.publications.create<MavenPublication>("maven") {
        artifact(tasks["remapJar"])
    }

    tasks {
        remapJar {
            archiveBaseName = if (project == project(":fabric:intermediary")) {
                "${rootProject.name}-fabric-intermediary"
            } else {
                "${rootProject.name}-fabric-${project.name}"
            }
            archiveVersion = rootProject.version as String
        }

        remapSourcesJar {
            archiveBaseName = if (project == project(":fabric:intermediary")) {
                "${rootProject.name}-fabric-intermediary"
            } else {
                "${rootProject.name}-fabric-${project.name}"
            }
            archiveVersion = rootProject.version as String
        }
    }
}

subprojects {
    dependencies {
        // configuration = "namedElements" required when depending on another loom project
        implementation(project(":fabric:intermediary", configuration = "namedElements"))
        // Shared NMS-free Fabric code (e.g. FabricFutureUtil) lives in fabric-common;
        // the per-version submodules reference it, so it must be on their compile path.
        compileOnly(project(":fabric:shared"))
        // PE is JiJ'd at fabric/ (aggregator); per-version submodules just need it on
        // the compile classpath. compileOnly avoids re-nesting PE inside each mcXXXX jar.
        compileOnly(files(rootProject.file("libs/packetevents-api.jar")))
        compileOnly(files(rootProject.file("libs/packetevents-fabric-common.jar")))
    }
}

subprojects.forEach {
    tasks.named("remapJar").configure {
        dependsOn("${it.path}:remapJar")
    }
}

tasks.remapJar.configure {
    subprojects.forEach { subproject ->
        subproject.tasks.matching { it.name == "remapJar" }.configureEach {
            nestedJars.from(this)
        }
    }
}
