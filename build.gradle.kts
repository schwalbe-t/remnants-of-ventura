
plugins {
    kotlin("jvm") version "2.2.21"
}

allprojects {

    apply(plugin = "org.jetbrains.kotlin.jvm")

    kotlin {
        jvmToolchain(21)

        compilerOptions {
            optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
            optIn.add("kotlin.uuid.ExperimentalUuidApi")
        }
    }

}

subprojects {

    repositories {
        mavenCentral()
    }

    val serialVersion = "1.9.0"
    val ktorVersion = "3.1.0"

    dependencies {
        implementation(kotlin("stdlib"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serialVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serialVersion")

        implementation("io.ktor:ktor-server-netty:$ktorVersion")
        implementation("io.ktor:ktor-server-websockets:$ktorVersion")
        implementation("io.ktor:ktor-client-core:$ktorVersion")
        implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
        implementation("io.ktor:ktor-client-websockets:$ktorVersion")
        implementation("io.ktor:ktor-network-tls:$ktorVersion")


        implementation("org.joml:joml:1.10.5")
    }

}