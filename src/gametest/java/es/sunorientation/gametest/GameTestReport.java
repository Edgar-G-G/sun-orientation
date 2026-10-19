package es.sunorientation.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import es.sunorientation.SunOrientation;
import es.sunorientation.config.SunOrientationConfig;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Checks and report shared by the in-game tests of every loader (Fabric client gametest and the NeoForge
 * test mod). The report is written to {@code sunorientation-results.txt} in the game folder.
 */
public final class GameTestReport {
    public static final int[] BEARINGS = {180, 0, 270, 45};
    public static final int[] TIMES = {0, 6000, 12000, 14000};

    private static final float EPS = 0.02F;

    private final List<String> report = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();

    /** Expected horizontal bearing of the light at a time of day, or -1 for noon (zenith). */
    public static int expectedLightBearing(int bearing, int time) {
        return time == 6000 ? -1 : time == 12000 ? bearing + 180 : bearing;
    }

    /** Calls the real Iris code (with this mod's mixins applied) through reflection. */
    public void checkIris(int bearing, int time) {
        int expectedBearing = expectedLightBearing(bearing, time);
        try {
            Class<?> cu = Class.forName("net.irisshaders.iris.uniforms.CelestialUniforms");
            Object uniforms = cu.getConstructor(float.class).newInstance(0.0F);
            Vector4f light4 = (Vector4f) cu.getMethod("getShadowLightPositionInWorldSpace").invoke(uniforms);
            Vector3f light = new Vector3f(light4.x, light4.y, light4.z).normalize();
            boolean isDay = (boolean) cu.getMethod("isDay").invoke(null);
            float sunAngle = (float) cu.getMethod("getSunAngle", boolean.class).invoke(null, isDay);
            String label = "bearing " + bearing + " t=" + time;
            log(String.format("Iris %s: isDay=%s light=(%.3f, %.3f, %.3f)", label, isDay, light.x, light.y, light.z));

            if (expectedBearing < 0) {
                expect(light.y > 0.95F, label + ": noon at the zenith " + light);
            } else {
                double r = Math.toRadians(expectedBearing);
                Vector3f horizontal = new Vector3f(light.x, 0, light.z).normalize();
                Vector3f want = new Vector3f((float) Math.sin(r), 0, (float) -Math.cos(r));
                expect(horizontal.distance(want) < 0.05F, label + ": light towards " + expectedBearing + "° " + horizontal);
            }

            Vector4f view4 = (Vector4f) cu.getMethod("getShadowLightPosition").invoke(uniforms);
            Class<?> crs = Class.forName("net.irisshaders.iris.uniforms.CapturedRenderingState");
            Object state = crs.getField("INSTANCE").get(null);
            Matrix4f gbuffer = new Matrix4f((Matrix4fc) crs.getMethod("getGbufferModelView").invoke(state));
            Vector3f viewToWorld = gbuffer.invert().transformDirection(new Vector3f(view4.x, view4.y, view4.z)).normalize();
            expect(viewToWorld.distance(light) < EPS, label + ": view position = world position " + viewToWorld);

            Class<?> sm = Class.forName("net.irisshaders.iris.shadows.ShadowMatrices");
            PoseStack pose = new PoseStack();
            sm.getMethod("createBaselineModelViewMatrix", PoseStack.class, float.class, float.class, float.class, float.class)
                .invoke(null, pose, sunAngle / 360.0F, 0.0F, -100.05F, 156.0F);
            Vector3f inShadowView = pose.last().pose().transformDirection(new Vector3f(light));
            expect(Math.abs(inShadowView.x) < EPS && Math.abs(inShadowView.y) < EPS && inShadowView.z > 1 - EPS,
                label + ": the shadow camera looks from the light " + inShadowView);
        } catch (ReflectiveOperationException e) {
            expect(false, "could not call Iris: " + e);
        }
    }

    /** Every vanilla hook must have run without shaders; with Iris, the Iris hooks must have run. */
    public void checkHooks(boolean iris) {
        Set<String> hooks = SunOrientation.activeHooks();
        log("active hooks: " + hooks);
        expectHook(hooks, SunOrientation.Hooks.CELESTIAL, !iris);
        expectHook(hooks, SunOrientation.Hooks.SUNRISE_GLOW, !iris);
        expectHook(hooks, SunOrientation.Hooks.FOG, !iris);
        if (iris) {
            expectHook(hooks, SunOrientation.Hooks.IRIS_UNIFORMS, true);
            expectHook(hooks, SunOrientation.Hooks.IRIS_SHADOWS, true);
            expectHook(hooks, SunOrientation.Hooks.SHADER_PATCH, true);
        }
    }

    public static String readConfigFile() {
        try {
            return Files.readString(SunOrientationConfig.path());
        } catch (IOException e) {
            return "(could not read: " + e + ")";
        }
    }

    private void expectHook(Set<String> hooks, String hook, boolean required) {
        if (hooks.contains(hook)) {
            log("OK hook: " + hook);
        } else if (required) {
            expect(false, "hook did not run: " + hook);
        } else {
            log("(hook did not run, expected with shaders: " + hook + ")");
        }
    }

    public void expect(boolean condition, String what) {
        if (condition) {
            log("OK " + what);
        } else {
            failures.add(what);
            log("FAILED " + what);
        }
    }

    public void log(String line) {
        report.add(line);
        SunOrientation.LOGGER.info("[SunOrientation-Test] {}", line);
    }

    public List<String> failures() {
        return List.copyOf(failures);
    }

    public void write() {
        report.add(failures.isEmpty() ? "RESULT: OK" : "RESULT: " + failures.size() + " FAILURE(S)");
        try {
            Files.write(Path.of("sunorientation-results.txt"), report);
        } catch (IOException e) {
            SunOrientation.LOGGER.warn("[SunOrientation-Test] could not write the report", e);
        }
    }
}
