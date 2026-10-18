package es.sunorientation.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import es.sunorientation.SkyRotation;
import es.sunorientation.SunOrientation;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Mod configuration, stored in {@code config/sun_orientation.json}:
 *
 * <pre>
 * { "sunriseBearing": 180, "adaptShaderPacks": true }
 * </pre>
 *
 * The mixins read the state every frame, so a change shows immediately; only the GLSL patch of shader
 * packs needs the pack to be reloaded.
 */
public final class SunOrientationConfig {
    public static final int DEFAULT_SUNRISE_BEARING = 180;
    public static final boolean DEFAULT_ADAPT_SHADER_PACKS = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Immutable state: render threads always see values that are consistent with each other. */
    public record State(int sunriseBearing, boolean adaptShaderPacks, float extraYaw, float sin, float cos) {
        static State of(int bearing, boolean adaptShaderPacks) {
            int b = SkyRotation.normalizeBearing(bearing);
            float yaw = SkyRotation.extraYawForBearing(b);
            return new State(b, adaptShaderPacks, yaw, SkyRotation.sinDegrees(yaw), SkyRotation.cosDegrees(yaw));
        }

        public int sunsetBearing() {
            return SkyRotation.normalizeBearing(sunriseBearing + 180);
        }

        public boolean isVanilla() {
            return extraYaw == 0.0F;
        }
    }

    private static volatile State state = State.of(DEFAULT_SUNRISE_BEARING, DEFAULT_ADAPT_SHADER_PACKS);
    private static boolean loaded;

    private SunOrientationConfig() {
    }

    public static State get() {
        return state;
    }

    /** Applies values live (without saving to disk). */
    public static void set(int sunriseBearing, boolean adaptShaderPacks) {
        state = State.of(sunriseBearing, adaptShaderPacks);
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(SunOrientation.MOD_ID + ".json");
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path file = path();
        if (!Files.exists(file)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            int bearing = json.has("sunriseBearing") ? json.get("sunriseBearing").getAsInt() : DEFAULT_SUNRISE_BEARING;
            boolean adapt = json.has("adaptShaderPacks") ? json.get("adaptShaderPacks").getAsBoolean() : DEFAULT_ADAPT_SHADER_PACKS;
            set(bearing, adapt);
            SunOrientation.LOGGER.info("[SunOrientation] the sun rises at {}° and sets at {}° (0 = north, 90 = east)",
                state.sunriseBearing(), state.sunsetBearing());
        } catch (RuntimeException | IOException e) {
            SunOrientation.LOGGER.warn("[SunOrientation] {} is not valid; using the defaults", file, e);
            set(DEFAULT_SUNRISE_BEARING, DEFAULT_ADAPT_SHADER_PACKS);
        }
    }

    public static synchronized void save() {
        JsonObject json = new JsonObject();
        json.addProperty("sunriseBearing", state.sunriseBearing());
        json.addProperty("adaptShaderPacks", state.adaptShaderPacks());
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            SunOrientation.LOGGER.warn("[SunOrientation] could not save {}", file, e);
        }
    }
}
