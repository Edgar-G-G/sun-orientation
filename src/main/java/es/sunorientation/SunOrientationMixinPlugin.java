package es.sunorientation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import es.sunorientation.config.SunOrientationConfig;
import es.sunorientation.platform.Platform;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Decides which mixins are applied. The Iris mixins are only applied when Iris is loaded,
 * so without Iris the mod never touches or tries to load any Iris class.
 */
public final class SunOrientationMixinPlugin implements IMixinConfigPlugin {
    /** Iris versions whose bytecode was reviewed for these mixins. */
    //? if >=26.3 {
    private static final String TESTED_IRIS_MIN = "1.11.7";
    //?} else
    //private static final String TESTED_IRIS_MIN = "1.11.4";
    private static final String TESTED_IRIS_MAX = "1.12";
    private static final String IRIS_MIXIN_PACKAGE = "es.sunorientation.mixin.iris.";

    private boolean irisLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        SunOrientationConfig.load();
        Optional<String> iris = Platform.modVersion("iris");
        irisLoaded = iris.isPresent();
        if (iris.isEmpty()) {
            SunOrientation.LOGGER.info("[SunOrientation] Iris is not installed: only the vanilla sky is rotated.");
            return;
        }

        String version = iris.get();
        if (Platform.isModVersionInRange("iris", TESTED_IRIS_MIN, TESTED_IRIS_MAX)) {
            SunOrientation.LOGGER.info("[SunOrientation] Iris {} detected: celestial uniforms and shadows are aligned.", version);
        } else {
            SunOrientation.LOGGER.warn("[SunOrientation] Iris {} is outside the reviewed range ({}). The Iris mixins are still"
                + " attempted; if no 'Iris ...' hooks appear in the log, the shader sun and shadows will not follow the sky.",
                version, "[" + TESTED_IRIS_MIN + ", " + TESTED_IRIS_MAX + ")");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(IRIS_MIXIN_PACKAGE)) {
            return irisLoaded;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        SunOrientation.LOGGER.info("[SunOrientation] mixin {} applied to {}", mixinClassName, targetClassName);
    }
}
