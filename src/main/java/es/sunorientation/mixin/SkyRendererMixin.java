package es.sunorientation.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import es.sunorientation.SunOrientation;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.3 {
import com.mojang.renderpearl.api.commands.RenderPass;
import org.joml.Vector4fc;
//?}

/**
 * Vanilla sky (bytecode reviewed for 26.3, 26.2 and 26.1.2):
 *
 * <pre>
 * renderSunMoonAndStars:  Ry(-90) · [Iris: Rz(sunPath)] · Rx(angle)
 * renderSunriseAndSunset: Rx(90) · Rz(angle + 90)
 * </pre>
 *
 * 26.3 writes each rotation as {@code PoseStack.rotateDegrees(Axis, float)}; 26.1 and 26.2 as
 * {@code PoseStack.mulPose(Axis.rotationDegrees(float))}, and their sunrise method has no render pass.
 *
 * <p>The first one already has a fixed Y rotation: the extra yaw is added to it (e.g. -90 → -180 for south),
 * which leaves the Iris injection right after it untouched. The second one has no Y rotation, so
 * {@code Ry(extra yaw)} is prepended to its first rotation; its {@code popPose} undoes it.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    //? if >=26.3 {
    @ModifyArg(
        method = "renderSunMoonAndStars",
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotateDegrees(Lcom/mojang/math/Axis;F)V",
            ordinal = 0),
        index = 1
    )
    //?} else {
    /*@ModifyArg(
        method = "renderSunMoonAndStars",
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;",
            ordinal = 0),
        index = 0
    )
    *///?}
    private float sunOrientation$rotateCelestialYaw(float degrees) {
        SunOrientation.markActive(SunOrientation.Hooks.CELESTIAL);
        return degrees + SunOrientation.extraYaw();
    }

    //? if >=26.3 {
    @Inject(
        method = "renderSunriseAndSunset",
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotateDegrees(Lcom/mojang/math/Axis;F)V",
            ordinal = 0)
    )
    private void sunOrientation$rotateSunriseFan(RenderPass renderPass, PoseStack poseStack, float sunAngle,
                                                 Vector4fc sunriseAndSunsetColor, CallbackInfo ci) {
        SunOrientation.markActive(SunOrientation.Hooks.SUNRISE_GLOW);
        float yaw = SunOrientation.extraYaw();
        if (yaw != 0.0F) {
            poseStack.rotateDegrees(Axis.YP, yaw);
        }
    }
    //?} else {
    /*@Inject(
        method = "renderSunriseAndSunset",
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V",
            ordinal = 0)
    )
    private void sunOrientation$rotateSunriseFan(PoseStack poseStack, float sunAngle, int sunriseAndSunsetColor,
                                                 CallbackInfo ci) {
        SunOrientation.markActive(SunOrientation.Hooks.SUNRISE_GLOW);
        float yaw = SunOrientation.extraYaw();
        if (yaw != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        }
    }
    *///?}
}
