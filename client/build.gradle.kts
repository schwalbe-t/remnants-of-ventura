
plugins {
    application
}

dependencies {
    implementation(project(":common"))
}

application {
    mainClass.set("schwalbe.ventura.client.MainKt")
}