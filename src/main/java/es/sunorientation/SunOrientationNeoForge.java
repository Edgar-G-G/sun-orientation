package es.sunorientation;

//? if neoforge {
/*import es.sunorientation.config.SunOrientationScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// NeoForge entry point (client only): the configuration button of the mods list. The mixins and the
// configuration are loaded by the mixin plugin, as on Fabric.
@Mod(value = SunOrientation.MOD_ID, dist = Dist.CLIENT)
public final class SunOrientationNeoForge {
    public SunOrientationNeoForge(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (IConfigScreenFactory) (mod, parent) -> new SunOrientationScreen(parent));
    }
}
*///?}
