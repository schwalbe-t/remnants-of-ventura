
import java.util.Properties

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
}

fun loadEnv(): Map<String, String> {
    val props = Properties()
    file(".env").inputStream().use { props.load(it) }
    return props.entries.associate { it.key.toString() to it.value.toString() }
}

subprojects {

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    kotlin {
        jvmToolchain(21)

        compilerOptions {
            optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
            optIn.add("kotlin.uuid.ExperimentalUuidApi")
        }
    }

    repositories {
        mavenCentral()
    }

    val serialVersion = "1.9.0"
    val ktorVersion = "3.1.0"

    dependencies {
        implementation(kotlin("stdlib"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serialVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:$serialVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialVersion")

        implementation("io.ktor:ktor-server-netty:$ktorVersion")
        implementation("io.ktor:ktor-server-websockets:$ktorVersion")
        implementation("io.ktor:ktor-client-core:$ktorVersion")
        implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
        implementation("io.ktor:ktor-client-websockets:$ktorVersion")
        implementation("io.ktor:ktor-network-tls:$ktorVersion")


        implementation("org.joml:joml:1.10.5")
    }

    tasks.withType<JavaExec> {
        environment(loadEnv())
    }

}