// Shared by every loader buildscript (build.gradle.kts, build.neoforge.gradle.kts, build.forge.gradle.kts).
// Applied with `apply(from = ...)` after the loader script has set `extra["modJar"]` (the jar to collect and upload).
// Script plugins get no type-safe accessors, hence `the<...>()` and `configure<...>`.
import com.modrinth.minotaur.ModrinthExtension
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import java.nio.file.Files

val sc = the<StonecutterBuildExtension>()
val loader = property("mod.loader") as String
val loaderName = mapOf("fabric" to "Fabric", "neoforge" to "NeoForge", "forge" to "Forge").getValue(loader)

// AbstractArchiveTask, not Jar: Loom's remapJar extends org.gradle.jvm.tasks.Jar, which isn't the script's `Jar`
@Suppress("UNCHECKED_CAST")
val modJar = extra["modJar"] as TaskProvider<out AbstractArchiveTask>

// Node name: "1.21.11" for Fabric, "1.21.1-neoforge" for the other loaders
version = "${property("mod.version")}+${sc.current.project}"
group = property("mod.group") as String
the<BasePluginExtension>().archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    else -> JavaVersion.VERSION_1_8
}

configure<JavaPluginExtension> {
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

// A GameTest server that stalls (seen once on 1.21.7) would otherwise run until the CI job limit; a node takes ~20 s
tasks.matching { it.name == "runGameTest" || it.name == "runGameTestServer" }.configureEach {
    timeout = java.time.Duration.ofMinutes(10)
}

// Test-only nodes are built and GameTested but neither collected nor uploaded
val publish = sc.properties.getOrNull<Boolean>("mod.publish") ?: true

// Minecraft releases to tag on Modrinth (TOML array, so read through the raw accessor)
val mcReleases = sc.properties.rawOrNull("mod", "mc_releases")?.asList()?.map { it.asPrimitive().content.toString() }
    ?: listOf(sc.current.version)

// Metadata files of each loader; only the node's own ones go in the jar
val loaderMetadata = mapOf(
    "fabric" to listOf("fabric.mod.json"),
    "neoforge" to listOf("META-INF/neoforge.mods.toml"),
    "forge" to listOf("META-INF/mods.toml"),
)

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    named<ProcessResources>("processResources") {
        val props = mapOf(
            "version" to project.version.toString(),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "loader" to (sc.properties.getOrNull<String>("deps.fabric_loader") ?: ""),
            "java" to requiredJava.majorVersion,
        )
        inputs.properties(props)

        filesMatching(loaderMetadata.getValue(loader)) { expand(props) }
        filesMatching("*.mixins.json") { expand(props) }
    }

    // Main and GameTest resources alike
    withType<ProcessResources>().configureEach {
        exclude(loaderMetadata.values.flatten() - loaderMetadata.getValue(loader))
    }

    withType<Jar>().configureEach {
        from(rootProject.file("LICENSE.txt"))
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to the root build/libs/{mod version}/"
        enabled = publish

        from(modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/${sc.properties.get<String>("mod.version")}"))
    }
}

// Client settings shared by every node through symlinks into run/shared/; saves/ stays per node.
// Not servers.dat: Minecraft saves it by replacing the file, which would replace the link.
val sharedRunFiles = mapOf(
    "options.txt" to false,
    "config" to true, "resourcepacks" to true, "screenshots" to true,
) // name to isDirectory

val linkSharedRunFiles by tasks.registering {
    group = "run"
    description = "Symlinks this node's client settings to run/shared/"

    val runDir = rootProject.file("run/${sc.current.project}").toPath()
    val sharedDir = rootProject.file("run/shared").toPath()
    val nodeName = sc.current.project
    doLast {
        Files.createDirectories(runDir)
        Files.createDirectories(sharedDir)
        for ((name, isDirectory) in sharedRunFiles) {
            val link = runDir.resolve(name)
            val target = sharedDir.resolve(name)
            if (Files.isSymbolicLink(link)) continue

            if (Files.exists(link)) {
                // The first node launched seeds the shared copy; later ones keep theirs aside
                if (Files.notExists(target)) Files.move(link, target)
                else Files.move(link, runDir.resolve("$name.unshared")).also {
                    logger.lifecycle("Kept $nodeName's own $name as $name.unshared")
                }
            } else if (isDirectory) {
                Files.createDirectories(target)
            }
            // Relative, so the links survive moving the repo
            Files.createSymbolicLink(link, runDir.relativize(target))
        }
    }
}

tasks.matching { it.name == "runClient" }.configureEach { dependsOn(linkSharedRunFiles) }

configure<ModrinthExtension> {
    token = providers.environmentVariable("MODRINTH_TOKEN")
    projectId = "oxidizing"
    versionNumber = project.version.toString()
    versionName = "${property("mod.version")} for $loaderName " +
        if (mcReleases.size == 1) mcReleases[0] else "${mcReleases.first()}-${mcReleases.last()}"
    versionType = "release"
    uploadFile.set(modJar)
    gameVersions.addAll(mcReleases)
    loaders.add(loader)
    // Quilt Loader runs Fabric mods; tagged only on the ranges it has been checked on
    if (loader == "fabric" && sc.properties.getOrNull<Boolean>("mod.quilt") == true) loaders.add("quilt")
    changelog = rootProject.file("CHANGELOG.md").takeIf { it.exists() }?.readText() ?: ""
    // `-PmodrinthDryRun` logs the request instead of uploading
    debugMode = providers.gradleProperty("modrinthDryRun").isPresent
}

tasks.named("modrinth") { enabled = publish }
