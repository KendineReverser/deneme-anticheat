plugins {
    deneme.`base-conventions`
}

dependencies {
    compileOnly(project(":api-shims"))
    compileOnly(libs.jetbrains.annotations)
}
