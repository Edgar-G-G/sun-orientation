package es.sunorientation;

import java.lang.reflect.Method;
import java.util.Optional;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Optional access to Iris through reflection: the mod does not depend on Iris at compile time or runtime.
 */
public final class IrisCompat {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("iris");

    private IrisCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    /** Name of the active pack, if Iris is installed and shaders are enabled. */
    public static Optional<String> activeShaderPack() {
        if (!LOADED) {
            return Optional.empty();
        }
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Object config = iris.getMethod("getIrisConfig").invoke(null);
            boolean enabled = (boolean) config.getClass().getMethod("areShadersEnabled").invoke(config);
            if (!enabled) {
                return Optional.empty();
            }
            Object name = iris.getMethod("getCurrentPackName").invoke(null);
            return Optional.ofNullable(name == null ? null : name.toString());
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Reloads the shader pack (so the GLSL patch uses the new angle). Call on the render thread. */
    public static boolean reloadShaderPack() {
        if (activeShaderPack().isEmpty()) {
            return false;
        }
        try {
            Method reload = Class.forName("net.irisshaders.iris.Iris").getMethod("reload");
            reload.invoke(null);
            SunOrientation.LOGGER.info("[SunOrientation] shader pack reloaded with the new orientation");
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            SunOrientation.LOGGER.warn("[SunOrientation] could not reload the shader pack", e);
            return false;
        }
    }
}
