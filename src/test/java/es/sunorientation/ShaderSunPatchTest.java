package es.sunorientation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Patch of the sun vector computed in GLSL (Complementary/BSL idiom). */
class ShaderSunPatchTest {
    private static final String MAT = "mat3(0.0, 0.0, 1.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0)";

    @Test
    void rotatesComplementarySunVector() {
        String line = "#define GetSunVector() normalize((gbufferModelView * vec4(vec3(-sin(overworldAngle), cos(overworldAngle) * SUN_ROTATION_DATA) * 2000.0, 1.0)).xyz)";
        String out = ShaderSunPatch.apply(line, "commonFunctions.glsl");
        assertEquals("#define GetSunVector() normalize((gbufferModelView * vec4((" + MAT
            + " * vec3(-sin(overworldAngle), cos(overworldAngle) * SUN_ROTATION_DATA)) * 2000.0, 1.0)).xyz)", out);
    }

    @Test
    void rotatesGbuffersSkybasicDisc() {
        String line = "vec3 rawSunVec2 = (gbufferModelView * vec4(vec3(-sin(ang), cos(ang) * sunRotationData2) * 2000.0, 1.0)).xyz;";
        assertTrue(ShaderSunPatch.apply(line, "gbuffers_skybasic.glsl").contains("(" + MAT + " * vec3(-sin(ang), cos(ang) * sunRotationData2))"));
    }

    @Test
    void leavesOtherFormsAlone() {
        String end = "#define GetSunVector() normalize((gbufferModelView * vec4(vec3(0.0, SUN_ROTATION_DATA * 2000.0), 1.0)).xyz)";
        assertSame(end, ShaderSunPatch.apply(end, "x"));
        String different = "vec3(-sin(a), cos(b) * r)";
        assertSame(different, ShaderSunPatch.apply(different, "x"));
    }

    @Test
    void matrixIsTheSameRotationAsTheSky() {
        // mat3 by columns: c0=(0,0,1), c1=(0,1,0), c2=(-1,0,0) → M·v = (-v.z, v.y, v.x).
        float[] v = {0.978F, 0.208F, 0.0F}; // Complementary at sunrise without the mod: east.
        float[] r = {-v[2], v[1], v[0]};
        assertEquals(SunOrientation.rotatedX(v[0], v[2]), r[0], 1e-6);
        assertEquals(SunOrientation.rotatedZ(v[0], v[2]), r[2], 1e-6);
        assertEquals(0.978F, r[2], 1e-6); // south
    }
}
