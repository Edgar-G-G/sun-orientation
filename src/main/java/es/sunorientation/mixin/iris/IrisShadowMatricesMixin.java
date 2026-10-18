package es.sunorientation.mixin.iris;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import es.sunorientation.SunOrientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shadow pass view of Iris 1.11.7 (26.3 branch). Outside the End with "end flash":
 *
 * <pre>
 * V = Rx(90) · Rz(-angle) · Rx(sunPath)          (rotateDegrees ordinals 2, 3 and 4)
 * </pre>
 *
 * {@code V} maps world space to the shadow camera. The sky and the light are rotated by
 * {@code R = Ry(yaw)} ({@code L' = R·L}), so the view needs the inverse rotation:
 * {@code V' = V · Ry(-yaw)}, which gives {@code V'·L' = V·L}. Since {@code rotateDegrees}
 * post-multiplies, it is enough to append {@code Ry(-yaw)} after the last rotation of that branch;
 * Iris then applies {@code snapModelViewToGrid}, which translates in world space and stays correct.
 *
 * <p>Using the same sign as the sky (as ClassicSunRotation does for its own direction) would make the
 * shadow camera look from the opposite side of the light.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shadows.ShadowMatrices", remap = false)
public abstract class IrisShadowMatricesMixin {
    @Inject(
        method = "createBaselineModelViewMatrix",
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotateDegrees(Lcom/mojang/math/Axis;F)V",
            ordinal = 4,
            shift = At.Shift.AFTER,
            remap = false),
        remap = false
    )
    private static void sunOrientation$alignShadowView(PoseStack target, float shadowAngle, float sunPathRotation,
                                                       float nearPlane, float farPlane, CallbackInfo ci) {
        SunOrientation.markActive(SunOrientation.Hooks.IRIS_SHADOWS);
        float yaw = SunOrientation.extraYaw();
        if (yaw != 0.0F) {
            target.rotateDegrees(Axis.YP, -yaw);
        }
    }
}
