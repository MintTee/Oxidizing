plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.11"

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    // Node names are "<mc>" for Fabric and "<mc>-<loader>" otherwise: `//? if neoforge {`
    val loader = node.metadata.project.substringAfter('-', "fabric")
    constants.match(loader, "fabric", "neoforge", "forge")
    // Annotation on each GameTest method (`/*$ gametest*/ @GameTest`). Before 1.21.5 Fabric needs an explicit
    // empty structure; NeoForge uses the test mod's own, and from 1.21.5 registers the tests in TestMod instead
    swaps["gametest"] = when {
        loader == "neoforge" && current.parsed >= "1.21.5" -> ""
        current.parsed >= "1.21.5" -> "@GameTest"
        loader == "neoforge" -> "@GameTest(template = \"empty\")"
        else -> "@GameTest(template = EMPTY_STRUCTURE)"
    }
}
