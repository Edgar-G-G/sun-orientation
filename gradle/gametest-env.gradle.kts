// Prepares the folder of the in-game test of a build node (applied by build.<loader>.gradle.kts, which sets
// extra["gameTestRunDir"] first). Development only: none of this goes into the .jar.
//
// Third-party jars are read from ../_shared/testenv/ (or -Ptestenv_dir=...), not versioned. Each node first looks
// in testenv/<node>/ (e.g. testenv/26.3-neoforge/) and falls back to testenv/ itself:
//   mods-iris/    Iris + Sodium          mods-nuit/    Nuit
//   mods-extra/   mods that are always loaded (e.g. Mod Menu)
//   shaderpacks/  shader pack enabled with Iris (the first one found; testenv/shaderpacks is shared)
// Variant: -Psunorientation.variant=vanilla|iris|nuit|iris-nuit|gallery|gallery-vanilla

val gameTestRunDir = extra["gameTestRunDir"] as File
val testEnvRoot = rootProject.file(providers.gradleProperty("testenv_dir").orElse("../_shared/testenv").get())
val gameTestVariant = providers.gradleProperty("sunorientation.variant").orElse("vanilla")

fun testEnvDir(name: String): File {
    val own = File(File(testEnvRoot, project.name), name)
    return if (own.isDirectory) own else File(testEnvRoot, name)
}

tasks.register("prepareGameTest") {
    val variant = gameTestVariant.get()
    val withIris = variant.contains("iris") || variant == "gallery"
    val withNuit = variant.contains("nuit")
    inputs.property("variant", variant)
    outputs.upToDateWhen { false }
    doLast {
        project.delete(gameTestRunDir)
        project.copy {
            from(testEnvDir("mods-iris")) { include(if (withIris) "*.jar" else "__none__") }
            from(testEnvDir("mods-nuit")) { include(if (withNuit) "*.jar" else "__none__") }
            from(testEnvDir("mods-extra")) { include("*.jar") }
            into(File(gameTestRunDir, "mods"))
        }
        if (withIris) {
            val packs = testEnvDir("shaderpacks").listFiles()?.filter { it.name.endsWith(".zip") }.orEmpty()
            if (packs.isEmpty()) {
                throw GradleException("Missing a shader pack in testenv/shaderpacks")
            }
            project.copy { from(packs[0]); into(File(gameTestRunDir, "shaderpacks")) }
            val cfg = File(gameTestRunDir, "config/iris.properties")
            cfg.parentFile.mkdirs()
            cfg.writeText("enableShaders=true\nshaderPack=${packs[0].name}\ndisableUpdateMessage=true\n")
        }
        logger.lifecycle("In-game test of ${project.name}, variant '$variant': " +
            (File(gameTestRunDir, "mods").list()?.joinToString(", ") ?: "no extra mods"))
    }
}

tasks.matching { it.name == "runClientGameTest" }.configureEach {
    dependsOn("prepareGameTest")
    (this as JavaExec).jvmArgs("-Dmixin.debug.countInjections=true", "-Dsunorientation.variant=${gameTestVariant.get()}")
}
