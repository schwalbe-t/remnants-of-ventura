
plugins {
    kotlin("jvm") version "2.2.21"
}

allprojects {

    apply(plugin = "org.jetbrains.kotlin.jvm")

    kotlin {
        jvmToolchain(21)
    }

}

subprojects {

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.9.0")

        implementation("org.joml:joml:1.10.5")
    }

}