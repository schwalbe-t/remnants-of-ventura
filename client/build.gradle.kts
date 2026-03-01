
plugins {
    application
}

val thisOsName = System.getProperty("os.name").lowercase()
val thisOsArch = System.getProperty("os.arch").lowercase()
val osName = System.getenv("VENTURA_BUILD_OS_NAME") ?: when {
    thisOsName.contains("win")
        -> "windows"
    thisOsName.contains("mac") && thisOsArch.contains("aarch64")
        -> "macos-arm64"
    thisOsName.contains("mac")
        -> "macos"
    thisOsArch.contains("aarch64") || thisOsArch.contains("arm")
        -> "linux-arm64"
    else
        -> "linux"
}

val lwjglVersion = "3.3.3"
val lwjglNatives = "natives-$osName"

dependencies {
    implementation(project(":common"))

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-stb")
    implementation("org.lwjgl:lwjgl-assimp")
    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-assimp::$lwjglNatives")
}

application {
    mainClass.set("schwalbe.ventura.client.MainKt")
}