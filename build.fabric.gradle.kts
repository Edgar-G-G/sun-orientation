// Build script of the Fabric nodes (versions/<minecraft>-fabric). Values come from stonecutter.properties.toml.
plugins {
    id("net.fabricmc.fabric-loom")
}

val minecraft = sc.current.version
val modVersion = sc.properties.get<String>("mod.version")

version = "$modVersion+$minecraft-fabric"
group = sc.properties.get<String>("mod.group")

base {
    archivesName = sc.properties.get<String>("mod.archives_name")
}

repositories {
    // Mod Menu (only to compile the integration; not a dependency of the mod)
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    implementation("net.fabricmc:fabric-loader:${sc.properties.get<String>("deps.fabric_loader")}")
    compileOnly("com.terraformersmc:modmenu:${sc.properties.get<String>("deps.modmenu")}")
    // No Fabric API or Iris as required dependencies.
    // The Iris mixins target classes by name and are skipped when Iris is missing.

    testImplementation(platform("org.junit:junit-bom:${sc.properties.get<String>("deps.junit")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed")
    }
}

// ---------------------------------------------------------------------------
// In-game test (Fabric client gametest). Development only: none of this goes into the .jar.
//   gradlew :26.3-fabric:runClientGameTest -Psunorientation.variant=vanilla|iris|nuit|iris-nuit|gallery|gallery-vanilla
// Third-party jars are read from ../_shared/testenv/ (or -Ptestenv_dir=...), not versioned:
//   testenv/mods-iris/    Iris + Sodium        testenv/mods-nuit/   Nuit
//   testenv/mods-extra/   mods that are always loaded (e.g. Mod Menu)
//   testenv/shaderpacks/  shader pack enabled with Iris (the first one found)
// ---------------------------------------------------------------------------
fabricApi {
    configureTests {
        createSourceSet = true
        modId = "sun_orientation_gametest"
        enableGameTests = false
        enableClientGameTests = true
        eula = true
        clearRunDirectory = false
    }
}

dependencies {
    "gametestImplementation"("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")
}

val gameTestRunDir = file("build/run/clientGameTest")
// Test jars and packs shared between mods (not versioned): ../_shared/testenv by default.
val testEnv = rootProject.file(providers.gradleProperty("testenv_dir").orElse("../_shared/testenv").get())
val gameTestVariant = providers.gradleProperty("sunorientation.variant").orElse("vanilla")

tasks.register("prepareGameTest") {
    val variant = gameTestVariant.get()
    val withIris = variant.contains("iris") || variant == "gallery"
    val withNuit = variant.contains("nuit")
    inputs.property("variant", variant)
    outputs.upToDateWhen { false }
    doLast {
        project.delete(gameTestRunDir)
        project.copy {
            from(File(testEnv, "mods-iris")) { include(if (withIris) "*.jar" else "__none__") }
            from(File(testEnv, "mods-nuit")) { include(if (withNuit) "*.jar" else "__none__") }
            from(File(testEnv, "mods-extra")) { include("*.jar") }
            into(File(gameTestRunDir, "mods"))
        }
        if (withIris) {
            val packs = File(testEnv, "shaderpacks").listFiles()?.filter { it.name.endsWith(".zip") }.orEmpty()
            if (packs.isEmpty()) {
                throw GradleException("Missing a shader pack in testenv/shaderpacks")
            }
            project.copy { from(packs[0]); into(File(gameTestRunDir, "shaderpacks")) }
            val cfg = File(gameTestRunDir, "config/iris.properties")
            cfg.parentFile.mkdirs()
            cfg.writeText("enableShaders=true\nshaderPack=${packs[0].name}\ndisableUpdateMessage=true\n")
        }
        logger.lifecycle("In-game test, variant '$variant': " +
            (File(gameTestRunDir, "mods").list()?.joinToString(", ") ?: "no extra mods"))
    }
}

tasks.matching { it.name == "runClientGameTest" }.configureEach {
    dependsOn("prepareGameTest")
    (this as JavaExec).jvmArgs("-Dmixin.debug.countInjections=true", "-Dsunorientation.variant=${gameTestVariant.get()}")
}

tasks.processResources {
    val repo = sc.properties.get<String>("mod.github_repo")
    val contact = linkedMapOf("homepage" to "https://modrinth.com/mod/sun-orientation")
    if (repo.isNotEmpty()) {
        contact["sources"] = "https://github.com/$repo"
        contact["issues"] = "https://github.com/$repo/issues"
    }
    val props = mapOf(
        "version" to modVersion,
        "contact" to groovy.json.JsonOutput.toJson(contact),
        "minecraft" to sc.properties.get<String>("mod.mc_compat"),
        "fabricloader" to sc.properties.get<String>("deps.fabric_loader"),
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
    exclude("META-INF/neoforge.mods.toml")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
    val projectName = base.archivesName.get()
    inputs.property("projectName", projectName)
    from(rootProject.file("LICENSE")) {
        rename { "${it}_$projectName" }
    }
}

// Every build also leaves a copy of the mod jar in release/ for easy access.
tasks.named("build") {
    val releaseDir = rootProject.file("release")
    val jarFile = tasks.jar.flatMap { it.archiveFile }
    doLast {
        project.copy {
            from(jarFile)
            into(releaseDir)
        }
    }
}
