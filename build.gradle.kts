import versioning.BuildConfig
import versioning.VersionUtil

BuildConfig.init(project)

val baseVersion = "0.1.0"
group = "ac.deneme"
version = VersionUtil.computeVersion(project, baseVersion)
description = "deneme anticheat - prediction based anticheat for Minecraft 1.21.11"

ext["timestamp"] = System.currentTimeMillis().toString()
ext["git_branch"] = VersionUtil.getGitBranch(project, true)
ext["git_commit"] = VersionUtil.getGitCommitHash(project, true)
ext["git_org"] = System.getenv("DENEME_GIT_ORG") ?: VersionUtil.getGitUser(project)
ext["git_repo"] = System.getenv("DENEME_GIT_REPO") ?: "deneme-anticheat"

tasks.register("printVersion") {
    group = "versioning"
    description = "Prints the computed project version"

    inputs.property("version", version.toString())

    doLast {
        println("VERSION=${inputs.properties["version"]}")
    }
}

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.isIncremental = true
    }
}
