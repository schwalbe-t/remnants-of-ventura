
plugins {
    application
}

dependencies {
    implementation(project(":client"))
}

application {
    mainClass.set("schwalbe.ventura.editor.MainKt")
}

tasks.run.get().workingDir = rootProject.projectDir.resolve("client")