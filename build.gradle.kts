// Fabric nodes. Shared setup (versioning, Java, resources, run files, Modrinth) is in gradle/common.gradle.kts.
plugins {
    // Applies the correct Loom variant for the node's Minecraft version
    id("dev.kikugie.loom-back-compat")
    // Uploads the jar to Modrinth (`modrinth` task)
    id("com.modrinth.minotaur")
}

extra["modJar"] = loomx.modJar
apply(from = rootProject.file("gradle/common.gradle.kts"))

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Mojang names on obfuscated versions too, so the source is the same on every node
    loomx.applyMojangMappings()

    // `mod*` configurations are converted by loom-back-compat on 26.1+
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    // Only the GameTests use Fabric API; the mod itself needs nothing but the loader
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
}

fabricApi {
    // Adds the `gametest` source set and a headless `runGameTest` task
    configureTests {
        createSourceSet = true
        modId = "oxidizing_test"
        enableClientGameTests = false
        eula = true
    }
}

loom {
    runConfigs.all {
        // Separate run directory per node: worlds can't be opened by an older Minecraft
        runDirectory = rootProject.file("run/${sc.current.project}")
        jvmArguments.add("-Dmixin.debug.export=true")
    }
}
