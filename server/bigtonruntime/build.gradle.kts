
plugins {
    kotlin("jvm")
    id("io.github.fletchmckee.ktjni") version "0.1.0"
    id("dev.nokee.jni-library")
    id("dev.nokee.c-language")
}

ktjni {
    outputDir = layout.projectDirectory.dir("src/main/c/jni/generated")
}

tasks.withType<CCompile>().configureEach {
    dependsOn("generateKotlinMainJniHeaders")
    mustRunAfter("generateKotlinMainJniHeaders")
    compilerArgs.addAll(listOf(
        "-O3",
        "-flto"
    ))
}

tasks.withType<AbstractLinkTask>().configureEach {
    linkerArgs.addAll(listOf(
        "-flto"
    ))
}