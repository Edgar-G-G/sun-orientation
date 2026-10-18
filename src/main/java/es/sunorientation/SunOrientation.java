package es.sunorientation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import es.sunorientation.config.SunOrientationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entry point for the mixins: the current sky rotation and the record of active hooks.
 *
 * <p>Every part that depends on the sun direction gets the same rotation {@code R = Ry(extraYaw)}
 * (see {@link SkyRotation}):
 * <ul>
 *   <li>vanilla: sun/moon/stars, sunrise glow and atmospheric fog;</li>
 *   <li>Iris: celestial positions ({@code sunPosition}, {@code moonPosition},
 *       {@code shadowLightPosition}) and the shadow view. The view maps world to camera space,
 *       so it gets the inverse rotation: {@code V' = V·R⁻¹};</li>
 *   <li>packs that compute the sun in GLSL: {@link ShaderSunPatch}.</li>
 * </ul>
 */
public final class SunOrientation {
    public static final String MOD_ID = "sun_orientation";
    public static final Logger LOGGER = LoggerFactory.getLogger("SunOrientation");

    /** Names of the hooks reported in the log the first time they run. */
    public static final class Hooks {
        public static final String CELESTIAL = "vanilla sun/moon/stars";
        public static final String SUNRISE_GLOW = "vanilla sunrise/sunset glow";
        public static final String FOG = "vanilla sunrise fog tint";
        public static final String IRIS_UNIFORMS = "Iris celestial positions";
        public static final String IRIS_SHADOWS = "Iris shadow matrix";
        public static final String SHADER_PATCH = "shader pack sun vector";

        private Hooks() {
        }
    }

    private static final Set<String> ACTIVE_HOOKS = ConcurrentHashMap.newKeySet();

    private SunOrientation() {
    }

    /** Current extra sky rotation, in degrees around +Y (0 = vanilla). */
    public static float extraYaw() {
        return SunOrientationConfig.get().extraYaw();
    }

    /** X component of {@code R·(x, y, z)} with the current rotation. */
    public static float rotatedX(float x, float z) {
        SunOrientationConfig.State s = SunOrientationConfig.get();
        return SkyRotation.rotatedX(x, z, s.sin(), s.cos());
    }

    /** Z component of {@code R·(x, y, z)} with the current rotation. */
    public static float rotatedZ(float x, float z) {
        SunOrientationConfig.State s = SunOrientationConfig.get();
        return SkyRotation.rotatedZ(x, z, s.sin(), s.cos());
    }

    /**
     * Logs, once per hook, that an injection has actually run. Useful to check in {@code latest.log}
     * which parts of the rotation are active.
     */
    public static void markActive(String hook) {
        if (ACTIVE_HOOKS.add(hook)) {
            LOGGER.info("[SunOrientation] hook active: {}", hook);
        }
    }

    /** Hooks that have run at least once (for diagnostics and in-game tests). */
    public static Set<String> activeHooks() {
        return Set.copyOf(ACTIVE_HOOKS);
    }
}
