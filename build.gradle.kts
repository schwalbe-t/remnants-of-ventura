
plugins {
    kotlin("jvm") version "1.9.23"
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

        implementation("org.joml:joml:1.10.5")
    }

}