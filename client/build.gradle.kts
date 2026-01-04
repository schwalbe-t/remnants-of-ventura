
plugins {
    application
}

val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()

val lwjglVersion = "3.3.3"
val lwjglNatives = "natives-" + when {
    osName.contains("win")                                  -> "windows"
    osName.contains("mac") && osArch.contains("aarch64")    -> "macos-arm64"
    osName.contains("mac")                                  -> "macos"
    osArch.contains("aarch64") || osArch.contains("arm")    -> "linux-arm64"
    else                                                    -> "linux"
}

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