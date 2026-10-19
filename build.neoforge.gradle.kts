// Build script of the NeoForge nodes (versions/<minecraft>-neoforge). Values come from stonecutter.properties.toml.
plugins {
    id("net.neoforged.moddev")
}

val minecraft = sc.current.version
val modVersion = sc.properties.get<String>("mod.version")

version = "$modVersion+$minecraft-neoforge"
group = sc.properties.get<String>("mod.group")

base {
    archivesName = sc.properties.get<String>("mod.archives_name")
}

// In-game test, development only (NeoForge has no client gametest like Fabric's; see NeoForgeGameTest):
//   gradlew :26.3-neoforge:runClientGameTest -Psunorientation.variant=vanilla|iris|nuit|iris-nuit
// Test jars and the shader pack come from _shared/testenv (see gradle/gametest-env.gradle.kts).
val gametest: SourceSet = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
val gameTestRunDir = file("build/run/clientGameTest")

neoForge {
    // -Psunorientation.neoforge=<version>: test against another NeoForge build (the jar's range does not change)
    version = providers.gradleProperty("sunorientation.neoforge").orElse(sc.properties.get<String>("deps.neoforge")).get()

    mods {
        register("sun_orientation") {
            sourceSet(sourceSets.main.get())
        }
        register("sun_orientation_gametest") {
            sourceSet(gametest)
        }
    }

    // Unit tests use the real Minecraft classes (PoseStack, Axis, JOML).
    addModdingDependenciesTo(sourceSets.test.get())
    addModdingDependenciesTo(gametest)

    runs {
        register("clientGameTest") {
            client()
            gameDirectory = gameTestRunDir
            sourceSet = gametest
            loadedMods.set(listOf(mods["sun_orientation"], mods["sun_orientation_gametest"]))
            systemProperty("sunorientation.variant",
                providers.gradleProperty("sunorientation.variant").orElse("vanilla").get())
            // NeoForge validates every draw strictly in development only and Iris fails that check (26.3:
            // CenterDepthSampler), which never happens in a normal game. The switch was renamed in 26.3.
            systemProperty("neoforge.disableBlaze3DValidation", "true")
            systemProperty("neoforge.disableGlValidation", "true")
        }
    }
}

tasks.named<ProcessResources>("processGametestResources") {
    exclude("fabric.mod.json")
}

extra["gameTestRunDir"] = gameTestRunDir
apply(from = rootProject.file("gradle/gametest-env.gradle.kts"))

dependencies {
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

tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}

// The NeoForm runtime tasks default to a Java 21 toolchain; use the JDK 25 of the project instead.
tasks.withType<net.neoforged.nfrtgradle.NeoFormRuntimeTask>().configureEach {
    javaExecutable = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }
        .map { it.executablePath.asFile.absolutePath }
}

tasks.processResources {
    val repo = sc.properties.get<String>("mod.github_repo")
    val props = mapOf(
        "version" to modVersion,
        "minecraft" to sc.properties.get<String>("mod.mc_compat"),
        "neoforge" to sc.properties.get<String>("mod.neoforge_compat"),
        "iris" to sc.properties.get<String>("mod.iris_compat"),
        "sources" to if (repo.isEmpty()) "" else "https://github.com/$repo",
        "issues" to if (repo.isEmpty()) "" else "https://github.com/$repo/issues",
    )
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
    exclude("fabric.mod.json")
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    // ModDevGradle runs its tools and the game with the project toolchain (the local JDK 25).
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
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
