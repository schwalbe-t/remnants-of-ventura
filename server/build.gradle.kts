
plugins {
    application
}

val exposedVersion = "0.51.1"

dependencies {
    implementation(project(":common"))
    implementation(project("bigtonruntime"))

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")

    implementation("org.postgresql:postgresql:42.7.3")

    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
}

application {
    mainClass.set("schwalbe.ventura.server.MainKt")
}

val bigtonRuntime = project(":server:bigtonruntime")

tasks.named("build") {
    dependsOn(bigtonRuntime.tasks.named("link"))
}
