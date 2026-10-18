package es.sunorientation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import es.sunorientation.config.SunOrientationConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Decides which mixins are applied. The Iris mixins are only applied when Iris is loaded,
 * so without Iris the mod never touches or tries to load any Iris class.
 */
public final class SunOrientationMixinPlugin implements IMixinConfigPlugin {
    /** Iris versions whose bytecode was reviewed for these mixins. */
    private static final String TESTED_IRIS = ">=1.11.7 <1.12";
    private static final String IRIS_MIXIN_PACKAGE = "es.sunorientation.mixin.iris.";

    private boolean irisLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        SunOrientationConfig.load();
        Optional<ModContainer> iris = FabricLoader.getInstance().getModContainer("iris");
        irisLoaded = iris.isPresent();
        if (iris.isEmpty()) {
            SunOrientation.LOGGER.info("[SunOrientation] Iris is not installed: only the vanilla sky is rotated.");
            return;
        }

        String version = iris.get().getMetadata().getVersion().getFriendlyString();
        boolean tested;
        try {
            tested = VersionPredicate.parse(TESTED_IRIS).test(iris.get().getMetadata().getVersion());
        } catch (VersionParsingException e) {
            tested = false;
        }
        if (tested) {
            SunOrientation.LOGGER.info("[SunOrientation] Iris {} detected: celestial uniforms and shadows are aligned.", version);
        } else {
            SunOrientation.LOGGER.warn("[SunOrientation] Iris {} is outside the reviewed range ({}). The Iris mixins are still"
                + " attempted; if no 'Iris ...' hooks appear in the log, the shader sun and shadows will not follow the sky.",
                version, TESTED_IRIS);
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
