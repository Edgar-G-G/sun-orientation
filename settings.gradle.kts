pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("net.fabricmc.fabric-loom") version "1.18-SNAPSHOT"
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
}

// One build node per Minecraft version and loader: versions/<minecraft>-<loader>, each with the build
// script of its loader (build.<loader>.gradle.kts). The source in src/ is the vcsVersion node; Stonecutter
// comments (//? if ...) switch the parts that differ.
stonecutter {
    create(rootProject) {
        fun match(version: String, vararg loaders: String) {
            for (loader in loaders) version("$version-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        match("26.3", "fabric")
        vcsVersion = "26.3-fabric"
    }
}

rootProject.name = "sun-orientation"
