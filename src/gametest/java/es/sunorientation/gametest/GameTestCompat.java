package es.sunorientation.gametest;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Client calls of the in-game tests that changed between Minecraft versions: 26.2 moved the screen, the
 * overlay and the HUD from {@code Minecraft} to {@code Gui}, and the main render target to {@code GameRenderer}.
 */
final class GameTestCompat {
    private GameTestCompat() {
    }

    static Screen screen() {
        //? if >=26.2 {
        return Minecraft.getInstance().gui.screen();
        //?} else
        //return Minecraft.getInstance().screen;
    }

    static void setScreen(Screen screen) {
        //? if >=26.2 {
        Minecraft.getInstance().gui.setScreen(screen);
        //?} else
        //Minecraft.getInstance().setScreen(screen);
    }

    /** True when no loading overlay (resource reload) is shown. */
    static boolean overlayGone(Minecraft mc) {
        //? if >=26.2 {
        return mc.gui.overlay() == null;
        //?} else
        //return mc.getOverlay() == null;
    }

    static void hideHud(Minecraft mc) {
        //? if >=26.2 {
        if (!mc.gui.hud.isHidden()) {
            mc.gui.hud.toggle();
        }
        //?} else
        //mc.options.hideGui = true;
    }

    static RenderTarget mainRenderTarget(Minecraft mc) {
        //? if >=26.2 {
        return mc.gameRenderer.mainRenderTarget();
        //?} else
        //return mc.getMainRenderTarget();
    }
}
