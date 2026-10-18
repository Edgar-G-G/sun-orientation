package es.sunorientation.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.commands.RenderPass;
import es.sunorientation.SunOrientation;
import net.minecraft.client.renderer.SkyRenderer;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla sky of Minecraft 26.3 (bytecode reviewed):
 *
 * <pre>
 * renderSunMoonAndStars:  rotateDegrees(YP, -90) · [Iris: rotateDegrees(ZP, sunPath)] · rotate(XP, angle)
 * renderSunriseAndSunset: rotateDegrees(XP, 90) · rotateDegrees(ZP, angle + 90)
 * </pre>
 *
 * The first one already has a fixed Y rotation: the extra yaw is added to it (e.g. -90 → -180 for south),
 * which leaves the Iris injection right after it untouched. The second one has no Y rotation, so
 * {@code Ry(extra yaw)} is prepended to its first rotation; its {@code popPose} undoes it.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    @ModifyArg(
        method = "renderSunMoonAndStars",
        at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotateDegrees(Lcom/mojang/math/Axis;F)V",
            ordinal = 0),
        index = 1
    )
    private float sunOrientation$rotateCelestialYaw(float degrees) {
        SunOrientation.markActive(SunOrientation.Hooks.CELESTIAL);
        return degrees + SunOrientation.extraYaw();
    }

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
}
