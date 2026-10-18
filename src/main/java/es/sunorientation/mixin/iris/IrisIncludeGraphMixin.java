package es.sunorientation.mixin.iris;

import java.nio.file.Path;

import es.sunorientation.ShaderSunPatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Iris reads every GLSL file of the pack in {@code IncludeGraph.readFile}. The sun vector computed in
 * GLSL (see {@link ShaderSunPatch}) is rewritten here before Iris processes it.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.shaderpack.include.IncludeGraph", remap = false)
public abstract class IrisIncludeGraphMixin {
    @Inject(method = "readFile", at = @At("RETURN"), cancellable = true, remap = false)
    private static void sunOrientation$patchSunVector(Path path, CallbackInfoReturnable<String> cir) {
        String source = cir.getReturnValue();
        String patched = ShaderSunPatch.apply(source, String.valueOf(path));
        if (patched != source) {
            cir.setReturnValue(patched);
        }
    }
}
