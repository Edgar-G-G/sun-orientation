package es.sunorientation.mixin.iris;

import es.sunorientation.SunOrientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Iris 1.11.7 (26.3 branch) computes the sun and moon positions on its own:
 *
 * <pre>
 * getCelestialPosition:            gbufferModelView · Ry(-90) · Rz(sunPath) · Rx(angle) · (0,100,0)
 * getCelestialPositionInWorldSpace:                   Ry(-90) · Rz(sunPath) · Rx(angle) · (0,100,0)
 * </pre>
 *
 * {@code sunPosition}, {@code moonPosition}, {@code shadowLightPosition} and its world-space version
 * (used by Iris for shadow culling) all come from these two methods, so adding the same extra yaw as in
 * vanilla to that constant keeps them consistent with the sky.
 *
 * <p>{@code getUpPosition} also uses {@code Ry(-90)}, but on the vertical vector, which a Y rotation does
 * not change. The End "end flash" uses other methods and is left alone.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CelestialUniforms", remap = false)
public abstract class IrisCelestialUniformsMixin {
    @ModifyConstant(
        method = {"getCelestialPosition", "getCelestialPositionInWorldSpace"},
        constant = @Constant(floatValue = -90.0F),
        remap = false
    )
    private float sunOrientation$rotateCelestialYaw(float degrees) {
        SunOrientation.markActive(SunOrientation.Hooks.IRIS_UNIFORMS);
        return degrees + SunOrientation.extraYaw();
    }
}
