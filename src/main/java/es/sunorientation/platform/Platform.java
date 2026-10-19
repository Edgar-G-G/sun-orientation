package es.sunorientation.platform;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The only loader-specific services the mod needs: the config folder and which mods are installed.
 * Stonecutter keeps the code of the loader of each build node.
 */
public final class Platform {
    private Platform() {
    }

    /** Name of the loader this jar was built for. */
    public static String loader() {
        //? if fabric {
        return "Fabric";
        //?} else {
        /*return "NeoForge";
        *///?}
    }

    public static Path configDir() {
        //? if fabric {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        //?} else {
        /*return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        *///?}
    }

    /** Works from the mixin plugin on, before the game starts. */
    public static boolean isModLoaded(String modId) {
        return modVersion(modId).isPresent();
    }

    /** Version of an installed mod, as written in its metadata. */
    public static Optional<String> modVersion(String modId) {
        //? if fabric {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(modId)
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString());
        //?} else {
        /*return net.neoforged.fml.loading.FMLLoader.getCurrent().getLoadingModList().getMods().stream()
            .filter(mod -> mod.getModId().equals(modId))
            .findFirst()
            .map(mod -> mod.getVersion().toString());
        *///?}
    }

    /**
     * Whether an installed mod is in {@code [min, maxExclusive)}. Build metadata after {@code +}
     * ({@code 1.11.7+mc26.3}) is ignored. False when the mod is missing or its version cannot be parsed.
     */
    public static boolean isModVersionInRange(String modId, String min, String maxExclusive) {
        //? if fabric {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(modId).map(mod -> {
            try {
                return net.fabricmc.loader.api.metadata.version.VersionPredicate
                    .parse(">=" + min + " <" + maxExclusive).test(mod.getMetadata().getVersion());
            } catch (net.fabricmc.loader.api.VersionParsingException e) {
                return false;
            }
        }).orElse(false);
        //?} else {
        /*return modVersion(modId).map(version -> {
            String plain = version.contains("+") ? version.substring(0, version.indexOf('+')) : version;
            try {
                return org.apache.maven.artifact.versioning.VersionRange
                    .createFromVersionSpec("[" + min + "," + maxExclusive + ")")
                    .containsVersion(new org.apache.maven.artifact.versioning.DefaultArtifactVersion(plain));
            } catch (org.apache.maven.artifact.versioning.InvalidVersionSpecificationException e) {
                return false;
            }
        }).orElse(false);
        *///?}
    }
}
