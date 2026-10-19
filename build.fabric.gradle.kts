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

// In-game test (Fabric client gametest), development only:
//   gradlew :26.3-fabric:runClientGameTest -Psunorientation.variant=vanilla|iris|nuit|iris-nuit|gallery|gallery-vanilla
// Test jars and the shader pack come from _shared/testenv (see gradle/gametest-env.gradle.kts).
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

tasks.named<ProcessResources>("processGametestResources") {
    exclude("META-INF/neoforge.mods.toml")
}

extra["gameTestRunDir"] = file("build/run/clientGameTest")
apply(from = rootProject.file("gradle/gametest-env.gradle.kts"))

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
