// NeoForge nodes. Shared setup (versioning, Java, resources, run files, Modrinth) is in gradle/common.gradle.kts.
plugins {
    // NeoForge's own Gradle plugin; Mojang names on every NeoForge version
    id("net.neoforged.moddev") version "2.0.148"
    // Uploads the jar to Modrinth (`modrinth` task)
    id("com.modrinth.minotaur")
}

extra["modJar"] = tasks.jar
apply(from = rootProject.file("gradle/common.gradle.kts"))

neoForge {
    version = property("deps.neoforge") as String

    runs {
        register("client") { client() }
        register("server") { server() }
        configureEach {
            // Separate run directory per node: worlds can't be opened by an older Minecraft
            gameDirectory = rootProject.file("run/${sc.current.project}")
            jvmArgument("-Dmixin.debug.export=true")
        }
    }

    mods {
        register("oxidizing") { sourceSet(sourceSets.main.get()) }
    }
}

// GameTests: the shared src/gametest/ as a test mod, run headless by `runGameTest`
val gametest: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

neoForge {
    addModdingDependenciesTo(gametest)

    runs {
        register("gameTestServer") {
            type = "gameTestServer"
            sourceSet = gametest
            systemProperty("neoforge.enabledGameTestNamespaces", "oxidizing_test")
        }
    }

    mods {
        register("oxidizing_test") { sourceSet(gametest) }
    }
}

tasks.register("runGameTest") {
    group = "verification"
    description = "Runs the GameTests on a headless server"
    dependsOn("runGameTestServer")
}
