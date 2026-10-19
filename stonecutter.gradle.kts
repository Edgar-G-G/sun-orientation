plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3-fabric"

stonecutter parameters {
    val (_, loader) = current.project.split('-', limit = 2)

    // Version- and loader-specific values of stonecutter.properties.toml
    properties {
        tags(current.version, loader)
    }

    // `//? if fabric {` / `//? if neoforge {` in the source
    constants {
        match(loader, "fabric", "neoforge")
    }
}
