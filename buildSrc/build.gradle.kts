plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.lombok)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}
