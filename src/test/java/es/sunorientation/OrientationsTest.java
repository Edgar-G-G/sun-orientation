package es.sunorientation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Any sunrise bearing (0 = north, 90 = east, 180 = south, 270 = west): the same checks as
 * {@link GeometryTest}, for every part the mod rotates.
 */
class OrientationsTest {
    private static final float EPS = 1.0E-3F;
    private static final float SUNRISE = 270.0F;
    private static final float SUNSET = 90.0F;

    /** Horizontal direction of a compass bearing in Minecraft axes. */
    private static Vector3f bearing(int b) {
        double r = Math.toRadians(b);
        return new Vector3f((float) Math.sin(r), 0, (float) -Math.cos(r));
    }

    private static Vector3f sky(float yaw, float angle) {
        PoseStack pose = new PoseStack();
        pose.rotateDegrees(Axis.YP, -90.0F + yaw);
        pose.rotate(Axis.XP, (float) Math.toRadians(angle));
        return pose.last().pose().transformPosition(new Vector3f(0, 100, 0)).normalize();
    }

    private static Vector3f irisLight(float yaw, float sunPath, float angle) {
        Matrix4f m = new Matrix4f().identity()
            .rotate(Axis.YP.rotationDegrees(-90.0F + yaw))
            .rotate(Axis.ZP.rotationDegrees(sunPath))
            .rotate(Axis.XP.rotationDegrees(angle));
        Vector4f p = m.transform(new Vector4f(0, 100, 0, 0));
        return new Vector3f(p.x, p.y, p.z).normalize();
    }

    private static Matrix4f shadowView(float angle, float sunPath, float yaw) {
        float c = angle + 90.0F;
        c = c < 0 ? c + 360 : c > 360 ? c - 360 : c;
        float shadowAngle = c / 360.0F;
        float skyAngle = shadowAngle < 0.25F ? shadowAngle + 0.75F : shadowAngle - 0.25F;
        PoseStack t = new PoseStack();
        t.rotateDegrees(Axis.XP, 90.0F);
        t.rotateDegrees(Axis.ZP, skyAngle * -360.0F);
        t.rotateDegrees(Axis.XP, sunPath);
        if (yaw != 0.0F) {
            t.rotateDegrees(Axis.YP, -yaw); // IrisShadowMatricesMixin
        }
        return new Matrix4f(t.last().pose());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 30, 45, 90, 135, 180, 200, 225, 270, 300, 315, 359})
    void sunRisesAndSetsWhereConfigured(int b) {
        float yaw = SkyRotation.extraYawForBearing(b);
        assertDir(bearing(b), sky(yaw, SUNRISE), "sunrise at " + b);
        assertDir(bearing(b + 180), sky(yaw, SUNSET), "sunset at " + (b + 180));
        assertDir(new Vector3f(0, 1, 0), sky(yaw, 0.0F), "noon at the zenith, bearing " + b);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 45, 90, 180, 225, 270, 333})
    void allPartsAgreeForAnyBearing(int b) {
        float yaw = SkyRotation.extraYawForBearing(b);
        float s = SkyRotation.sinDegrees(yaw);
        float c = SkyRotation.cosDegrees(yaw);

        // Sunrise glow: Ry(yaw) · Rx(90) · Rz(270) · (0,100,0).
        PoseStack fan = new PoseStack();
        fan.rotateDegrees(Axis.YP, yaw);
        fan.rotateDegrees(Axis.XP, 90.0F);
        fan.rotateDegrees(Axis.ZP, 270.0F);
        assertDir(bearing(b), fan.last().pose().transformPosition(new Vector3f(0, 100, 0)).normalize(), "glow " + b);

        // Fog: (sunX, 0, 0) rotated.
        assertDir(bearing(b), new Vector3f(SkyRotation.rotatedX(1, 0, s, c), 0, SkyRotation.rotatedZ(1, 0, s, c)), "fog " + b);

        // Iris: same light as the sky, with and without sunPathRotation, and shadows looking from the light.
        for (float sunPath : new float[] {0.0F, -40.0F, 25.0F}) {
            for (float a = 0.0F; a < 360.0F; a += 30.0F) {
                Vector3f light = irisLight(yaw, sunPath, a);
                if (sunPath == 0.0F) {
                    assertDir(sky(yaw, a), light, "Iris = sky, bearing " + b + " angle " + a);
                }
                Vector3f expected = shadowView(a, sunPath, 0.0F).transformDirection(irisLight(0.0F, sunPath, a));
                Vector3f got = shadowView(a, sunPath, yaw).transformDirection(light);
                assertDir(expected, got, "shadows, bearing " + b + " angle " + a + " sunPath " + sunPath);
            }
        }

        // GLSL patch: the generated matrix is the same rotation.
        if (yaw != 0.0F) {
            Matrix3f m = parseMat3(ShaderSunPatch.rotationMatrix(yaw));
            Vector3f v = new Vector3f(0.978F, 0.208F, 0.0F); // Complementary vector at sunrise (east)
            Vector3f ref = new Matrix4f().rotateY((float) Math.toRadians(yaw)).transformDirection(new Vector3f(v));
            assertDir(ref, m.transform(new Vector3f(v)), "GLSL mat3, bearing " + b);
        }
    }

    @Test
    void eastIsVanillaAndNotPatched() {
        assertEquals(0.0F, SkyRotation.extraYawForBearing(90));
        String src = "vec3(-sin(a), cos(a) * r)";
        assertEquals(src, ShaderSunPatch.apply(src, "x", 0.0F));
        assertEquals(-90.0F, SkyRotation.extraYawForBearing(180));
        assertEquals(90.0F, SkyRotation.extraYawForBearing(0));
        assertEquals(180.0F, SkyRotation.extraYawForBearing(270));
        assertEquals(180.0F, SkyRotation.extraYawForBearing(-90));
    }

    /** GLSL {@code mat3(c0x, c0y, c0z, c1x, ...)} by columns → JOML Matrix3f (also by columns). */
    private static Matrix3f parseMat3(String glsl) {
        Matcher m = Pattern.compile("-?\\d+\\.\\d+").matcher(glsl);
        float[] v = new float[9];
        for (int i = 0; i < 9; i++) {
            m.find();
            v[i] = Float.parseFloat(m.group());
        }
        return new Matrix3f(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]);
    }

    private static void assertDir(Vector3f want, Vector3f got, String what) {
        assertEquals(want.x, got.x, EPS, what + " (x) → " + got);
        assertEquals(want.y, got.y, EPS, what + " (y) → " + got);
        assertEquals(want.z, got.z, EPS, what + " (z) → " + got);
    }
}
