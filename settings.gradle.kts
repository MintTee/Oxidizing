pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

// The settings plugins below pull ASM 9.5 onto this classloader, which Loom's classloader asks first:
// ASM 9.5 can't read Java 25 classes, so Loom silently drops every class when it merges a 26.x jar
buildscript {
    repositories { mavenCentral() }
    dependencies {
        classpath("org.ow2.asm:asm:9.10.1")
        classpath("org.ow2.asm:asm-tree:9.10.1")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Applies the remapping Loom on obfuscated versions (<26.1) and the plain one on 26.1+
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    // Lets Gradle download the JDK each Minecraft version needs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    // Uploads to Modrinth. Declared here so gradle/common.gradle.kts (a script plugin) can see its classes
    id("com.modrinth.minotaur") version "2.8.7" apply false
}

stonecutter {
    create(rootProject) {
        // Published nodes cover a range (mod.mc_compat in stonecutter.properties.toml).
        // Test-only nodes (mod.publish = false) build and GameTest each release inside that range.
        versions(
            "1.18", "1.18.1", "1.18.2",
            "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4",
            "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
            "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5",
            "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11",
            "26.1", "26.1.1", "26.1.2", "26.2", "26.3",
        )
        // Other loaders: "<mc>-<loader>" nodes, each loader with its own buildscript
        versions(
            "1.21.1-neoforge" to "1.21.1", "1.21.11-neoforge" to "1.21.11",
            "26.1-neoforge" to "26.1", "26.1.1-neoforge" to "26.1.1", "26.1.2-neoforge" to "26.1.2",
            "26.2-neoforge" to "26.2", "26.3-neoforge" to "26.3",
        ).buildscript("build.neoforge.gradle.kts")
        vcsVersion = "1.21.11"
    }
}

rootProject.name = "oxidizing"
