package es.sunorientation;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.sunorientation.config.SunOrientationConfig;

/**
 * Some shader packs do not use {@code sunPosition}: they compute the sun vector in GLSL with a fixed
 * east-west formula. Complementary Reimagined (and BSL, which it derives from) do it like this:
 *
 * <pre>
 * gbufferModelView * vec4(vec3(-sin(ang), cos(ang) * sunRotationData) * 2000.0, 1.0)
 * </pre>
 *
 * The vector in parentheses is in world space and matches vanilla. This patch wraps it with the current
 * rotation of the mod, {@code mat3(Ry(yaw)) * vec3(...)}, so the pack lighting follows the same sun as the
 * sky and the shadows. The rotation is written into the pack source, so changing the orientation requires
 * reloading the pack (the configuration screen does it automatically).
 *
 * <p>Only that exact idiom is recognized (same identifier in {@code sin} and {@code cos}); any other way of
 * computing the sun is left untouched. It can be disabled in the configuration or with
 * {@code -Dsunorientation.shaderPatch=false}.
 */
public final class ShaderSunPatch {
    public static final boolean ALLOWED = !"false".equalsIgnoreCase(System.getProperty("sunorientation.shaderPatch"));

    private static final Pattern SUN_VECTOR = Pattern.compile(
        "vec3\\(\\s*-\\s*sin\\(\\s*(\\w+)\\s*\\)\\s*,\\s*cos\\(\\s*\\1\\s*\\)\\s*\\*\\s*(\\w+)\\s*\\)");

    private ShaderSunPatch() {
    }

    /** Applies the rotation of the current configuration. */
    public static String apply(String source, String fileName) {
        SunOrientationConfig.State state = SunOrientationConfig.get();
        if (!ALLOWED || !state.adaptShaderPacks()) {
            return source;
        }
        return apply(source, fileName, state.extraYaw());
    }

    /** Returns the source with the rotation applied, or the same object if there was nothing to change. */
    public static String apply(String source, String fileName, float extraYaw) {
        if (extraYaw == 0.0F || source == null || source.indexOf("sin(") < 0) {
            return source;
        }
        Matcher m = SUN_VECTOR.matcher(source);
        StringBuilder out = null;
        String rotation = null;
        int count = 0;
        while (m.find()) {
            if (out == null) {
                out = new StringBuilder(source.length() + 64);
                rotation = rotationMatrix(extraYaw);
            }
            m.appendReplacement(out, Matcher.quoteReplacement("(" + rotation + " * " + m.group() + ")"));
            count++;
        }
        if (out == null) {
            return source;
        }
        m.appendTail(out);
        SunOrientation.LOGGER.info("[SunOrientation] shader {}: {} sun vector(s) rotated by {}°", fileName, count, extraYaw);
        SunOrientation.markActive(SunOrientation.Hooks.SHADER_PATCH);
        return out.toString();
    }

    /** {@code Ry(yaw)} in GLSL; {@code mat3} is built by columns: (c,0,-s), (0,1,0), (s,0,c). */
    public static String rotationMatrix(float extraYaw) {
        float c = SkyRotation.cosDegrees(extraYaw);
        float s = SkyRotation.sinDegrees(extraYaw);
        return String.format(Locale.ROOT, "mat3(%s, 0.0, %s, 0.0, 1.0, 0.0, %s, 0.0, %s)",
            glsl(c), glsl(-s), glsl(s), glsl(c));
    }

    private static String glsl(float v) {
        if (v == 0.0F) {
            return "0.0";
        }
        String text = String.format(Locale.ROOT, "%.7f", v).replaceAll("0+$", "");
        return text.endsWith(".") ? text + "0" : text;
    }
}
