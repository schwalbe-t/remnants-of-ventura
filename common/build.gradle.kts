
dependencies {}

val versionMajor: String by project
val versionMinor: String by project
val versionPatch: String by project

val generatedVersionDir = layout.buildDirectory.dir("generated-src/version")

sourceSets.main {
    java.srcDir(generatedVersionDir)
}

tasks.register("generateVersionClass") {
    val outputDir = generatedVersionDir.get().asFile
    outputs.dir(outputDir)

    doLast {
        val file = outputDir.resolve("schwalbe/ventura/Version.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
                package schwalbe.ventura
                
                object Version {
                    const val MAJOR: Int = $versionMajor
                    const val MINOR: Int = $versionMinor
                    const val PATCH: Int = $versionPatch
                }
            """.trimIndent()
        )
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateVersionClass")
}