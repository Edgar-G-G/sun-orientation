package es.sunorientation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks the geometry with the real Minecraft 26.3 classes ({@code PoseStack}, {@code Axis}) and JOML,
 * reproducing the transformation sequence of the reviewed vanilla and Iris 1.11.7 bytecode, for the default
 * orientation (sunrise in the south). It does not replace the in-game test: it validates signs and order.
 *
 * <p>Axes: east = +X, west = -X, south = +Z, north = -Z. SUN_ANGLE (degrees): 270 at sunrise
 * (tick 0), 0 at noon (6000), 90 at sunset (12000).
 */
class GeometryTest {
    private static final float EPS = 1.0E-3F;
    /** Rotation of the default orientation (sunrise in the south). */
    private static final float YAW = SkyRotation.extraYawForBearing(180);
    private static final float SUNRISE = 270.0F;
    private static final float NOON = 0.0F;
    private static final float SUNSET = 90.0F;

    // ---------- Vanilla ----------

    /** SkyRenderer.renderSunMoonAndStars: rotateDegrees(YP, yaw) · rotate(XP, angle) · (0,100,0). */
    private static Vector3f vanillaSun(float yawDegrees, float sunAngleDegrees) {
        PoseStack pose = new PoseStack();
        pose.rotateDegrees(Axis.YP, yawDegrees);
        pose.rotate(Axis.XP, (float) Math.toRadians(sunAngleDegrees));
        return pose.last().pose().transformPosition(new Vector3f(0, 100, 0)).normalize();
    }

    @Test
    void vanillaRisesInTheEast() {
        assertDir(1, 0, 0, vanillaSun(-90.0F, SUNRISE), "vanilla: sunrise in the east");
        assertDir(-1, 0, 0, vanillaSun(-90.0F, SUNSET), "vanilla: sunset in the west");
    }

    @Test
    void defaultRisesSouthAndSetsNorth() {
        float yaw = -90.0F + YAW;
        assertDir(0, 0, 1, vanillaSun(yaw, SUNRISE), "sunrise in the south");
        assertDir(0, 1, 0, vanillaSun(yaw, NOON), "noon at the zenith");
        assertDir(0, 0, -1, vanillaSun(yaw, SUNSET), "sunset in the north");
    }

    /** SkyRenderer.renderSunriseAndSunset with the extra Ry prepended; fan center at (0,100,0). */
    private static Vector3f sunriseFan(float sunAngleDegrees, boolean modded) {
        PoseStack pose = new PoseStack();
        pose.pushPose();
        if (modded) {
            pose.rotateDegrees(Axis.YP, YAW);
        }
        pose.rotateDegrees(Axis.XP, 90.0F);
        float angle = (float) Math.sin(Math.toRadians(sunAngleDegrees)) < 0.0F ? 180.0F : 0.0F;
        pose.rotateDegrees(Axis.ZP, angle + 90.0F);
        return pose.last().pose().transformPosition(new Vector3f(0, 100, 0)).normalize();
    }

    @Test
    void sunriseGlowFollowsTheSun() {
        assertDir(1, 0, 0, sunriseFan(SUNRISE, false), "vanilla: glow in the east");
        assertDir(0, 0, 1, sunriseFan(SUNRISE, true), "sunrise glow in the south");
        assertDir(0, 0, -1, sunriseFan(SUNSET, true), "sunset glow in the north");
    }

    /** AtmosphericFogEnvironment: direction (sunX,0,0) rotated by the mod's WrapOperation. */
    @Test
    void sunriseFogFollowsTheSun() {
        float sunriseX = 1.0F; // Mth.sin(270°) < 0
        float sunsetX = -1.0F; // Mth.sin(90°) > 0
        assertDir(0, 0, 1, new Vector3f(SunOrientation.rotatedX(sunriseX, 0), 0, SunOrientation.rotatedZ(sunriseX, 0)), "fog south");
        assertDir(0, 0, -1, new Vector3f(SunOrientation.rotatedX(sunsetX, 0), 0, SunOrientation.rotatedZ(sunsetX, 0)), "fog north");
        // Same convention as Matrix4f.rotateY (the sky's).
        Vector3f v = new Vector3f(0.3F, 0.5F, -0.8F);
        Vector3f ref = new Matrix4f().rotateY((float) Math.toRadians(YAW)).transformDirection(new Vector3f(v));
        assertDir(ref.x, ref.y, ref.z, new Vector3f(SunOrientation.rotatedX(v.x, v.z), v.y, SunOrientation.rotatedZ(v.x, v.z)), "rotatedX/Z = rotateY");
    }

    // ---------- Iris 1.11.7 ----------

    /** CelestialUniforms.getCelestialPositionInWorldSpace with the given fixed Y rotation. */
    private static Vector3f irisLight(float yawDegrees, float sunPathRotation, float angleDegrees) {
        Matrix4f celestial = new Matrix4f().identity();
        celestial.rotate(Axis.YP.rotationDegrees(yawDegrees));
        celestial.rotate(Axis.ZP.rotationDegrees(sunPathRotation));
        celestial.rotate(Axis.XP.rotationDegrees(angleDegrees));
        Vector4f p = celestial.transform(new Vector4f(0, 100, 0, 0));
        return new Vector3f(p.x, p.y, p.z).normalize();
    }

    /** CelestialUniforms.getSunAngle(..) / 360, as used by ShadowRenderer.getShadowAngle(). */
    private static float shadowAngle(float angleDegrees) {
        float c = angleDegrees + 90.0F;
        if (c < 0) {
            c += 360;
        } else if (c > 360) {
            c -= 360;
        }
        return c / 360.0F;
    }

    /** ShadowMatrices.createBaselineModelViewMatrix (non-End branch) + optional extra yaw after ordinal 4. */
    private static Matrix4f shadowView(float shadowAngle, float sunPathRotation, Float extraYaw) {
        PoseStack target = new PoseStack();
        target.last().normal().identity();
        target.last().pose().identity();
        float skyAngle = shadowAngle < 0.25F ? shadowAngle + 0.75F : shadowAngle - 0.25F;
        target.rotateDegrees(Axis.XP, 90.0F);
        target.rotateDegrees(Axis.ZP, skyAngle * -360.0F);
        target.rotateDegrees(Axis.XP, sunPathRotation);
        if (extraYaw != null) {
            target.rotateDegrees(Axis.YP, extraYaw);
        }
        return new Matrix4f(target.last().pose());
    }

    @Test
    void irisWorldLightMatchesTheVanillaSky() {
        float yaw = -90.0F + YAW;
        for (float a : new float[] {SUNRISE, NOON, SUNSET, 300.0F, 45.0F}) {
            Vector3f sky = vanillaSun(yaw, a);
            assertDir(sky.x, sky.y, sky.z, irisLight(yaw, 0.0F, a), "sunPosition = vanilla sky, angle " + a);
        }
        assertDir(0, 0, 1, irisLight(yaw, 0.0F, SUNRISE), "Iris: sunrise in the south");
        assertDir(0, 0, -1, irisLight(yaw, 0.0F, SUNSET), "Iris: sunset in the north");
    }

    @ParameterizedTest
    @ValueSource(floats = {0.0F, -40.0F, 25.0F})
    void shadowsLookFromTheRotatedLight(float sunPathRotation) {
        for (float a = 0.0F; a < 360.0F; a += 15.0F) {
            Matrix4f vanillaView = shadowView(shadowAngle(a), sunPathRotation, null);
            Vector3f vanillaLight = irisLight(-90.0F, sunPathRotation, a);
            Vector3f expected = vanillaView.transformDirection(new Vector3f(vanillaLight));

            Matrix4f view = shadowView(shadowAngle(a), sunPathRotation, -YAW);
            Vector3f light = irisLight(-90.0F + YAW, sunPathRotation, a);
            Vector3f got = view.transformDirection(new Vector3f(light));

            assertDir(expected.x, expected.y, expected.z, got, "light in shadow space, angle " + a + ", sunPath " + sunPathRotation);
        }
    }

    @Test
    void untiltedShadowsLookAlongZ() {
        for (float a : new float[] {SUNRISE, NOON, SUNSET, 300.0F}) {
            Matrix4f view = shadowView(shadowAngle(a), 0.0F, -YAW);
            Vector3f got = view.transformDirection(irisLight(-90.0F + YAW, 0.0F, a));
            assertDir(0, 0, 1, got, "the shadow camera looks from the light, angle " + a);
        }
    }

    @Test
    void usingTheSkySignForShadowsWouldBeWrong() {
        Matrix4f view = shadowView(shadowAngle(SUNRISE), 0.0F, -90.0F);
        Vector3f got = view.transformDirection(irisLight(-180.0F, 0.0F, SUNRISE));
        assertTrue(got.z < -0.99F, "with Ry(-90) the sunrise light would be behind the shadow camera: " + got);
    }

    @Test
    void shadowYawIsTheInverseOfTheSky() {
        // V'·(R·w) = V·w for any w: the shadow scene turns rigidly with the sky.
        Matrix4f v = shadowView(shadowAngle(SUNRISE + 10), 15.0F, null);
        Matrix4f vPrime = shadowView(shadowAngle(SUNRISE + 10), 15.0F, -YAW);
        Matrix4f r = new Matrix4f().rotateY((float) Math.toRadians(YAW));
        Vector3f w = new Vector3f(0.2F, -0.7F, 0.4F);
        Vector3f a = v.transformDirection(new Vector3f(w));
        Vector3f b = vPrime.transformDirection(r.transformDirection(new Vector3f(w)));
        assertDir(a.x, a.y, a.z, b, "V'·R = V");
    }

    private static void assertDir(float x, float y, float z, Vector3f got, String what) {
        assertEquals(x, got.x, EPS, what + " (x) → " + got);
        assertEquals(y, got.y, EPS, what + " (y) → " + got);
        assertEquals(z, got.z, EPS, what + " (z) → " + got);
    }
}
