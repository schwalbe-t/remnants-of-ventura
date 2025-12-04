
plugins {
    application
}

dependencies {
    implementation(project(":common"))

    implementation("org.jetbrains.exposed:exposed-core:0.51.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.51.1")

    implementation("org.postgresql:postgresql:42.7.3")
}

application {
    mainClass.set("schwalbe.ventura.server.MainKt")
}