package es.sunorientation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import es.sunorientation.SunOrientation;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The atmospheric fog takes the sunrise color when the camera looks towards {@code (sunX, 0, 0)},
 * with sunX = +1 (east) at sunrise and -1 (west) at sunset. That vector gets the same rotation as the
 * sky, so the tint follows the configured direction.
 *
 * <p>Nuit wraps a different call of the same method ({@code getValue}, ordinal 1); both
 * {@code WrapOperation}s chain without conflict.
 */
@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogEnvironmentMixin {
    @WrapOperation(
        method = "getBaseColor",
        at = @At(value = "INVOKE", target = "Lorg/joml/Vector3fc;dot(FFF)F")
    )
    private float sunOrientation$rotateSunriseFogDirection(Vector3fc forward, float x, float y, float z,
                                                           Operation<Float> original) {
        SunOrientation.markActive(SunOrientation.Hooks.FOG);
        return original.call(forward, SunOrientation.rotatedX(x, z), y, SunOrientation.rotatedZ(x, z));
    }
}
