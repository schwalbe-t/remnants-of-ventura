
plugins {
    id("dev.nokee.jni-library")
    id("dev.nokee.c-language")
    id("java")
}

tasks.withType<org.gradle.language.c.tasks.CCompile>().configureEach {
    // compilerArgs.addAll(listOf(
    //     "-O3",
    //     "-flto"
    // ))
}

tasks.withType<org.gradle.nativeplatform.tasks.AbstractLinkTask>().configureEach {
    // linkerArgs.addAll(listOf(
    //     "-flto"
    // ))
}

val currentOs = org.gradle.internal.os.OperatingSystem.current()

val libPrefix = when {
    currentOs.isWindows -> ""
    currentOs.isMacOsX  -> "lib"
    currentOs.isLinux   -> "lib"
    else -> throw GradleException("Unsupported OS")
}
val libExt = when {
    currentOs.isWindows -> "dll"
    currentOs.isMacOsX  -> "dylib"
    currentOs.isLinux   -> "so"
    else -> throw GradleException("Unsupported OS")
}

val libName = "bigtonruntime"

val copyNativeLibs by tasks.registering(Copy::class) {
    from(layout.buildDirectory.dir("libs/main")) {
        include("$libPrefix$libName.$libExt")
    }
    into(projectDir)
    dependsOn("link")
}

tasks.named("processResources") {
    dependsOn(copyNativeLibs)
}
