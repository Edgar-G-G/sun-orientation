package es.sunorientation.gametest;

//? if fabric {
import java.util.List;
import java.util.concurrent.CompletableFuture;

import es.sunorientation.IrisCompat;
import es.sunorientation.SunOrientation;
import es.sunorientation.config.SunOrientationConfig;
import es.sunorientation.config.SunOrientationScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * In-game test (development only). Variants ({@code -Psunorientation.variant=...}):
 * <ul>
 *   <li>{@code vanilla}, {@code iris}, {@code nuit}, {@code iris-nuit}: flat world with a pillar; for
 *       several sunrise bearings it checks with the real code (and the already patched Iris) that the light
 *       rises there and sets on the opposite side, that shadows look from the light and that the hooks
 *       run; it also tests the configuration screen (buttons, Cancel, Done) and the Spanish translation;</li>
 *   <li>{@code gallery}, {@code gallery-vanilla}: normal world and 1920x1080 screenshots for the mod page.</li>
 * </ul>
 * The checks and the report ({@code sunorientation-results.txt}) are in {@link GameTestReport}, shared with
 * the NeoForge test.
 */
public class SunOrientationClientGameTest implements FabricClientGameTest {
    private final GameTestReport report = new GameTestReport();

    @Override
    public void runTest(ClientGameTestContext context) {
        String variant = System.getProperty("sunorientation.variant", "vanilla");
        boolean iris = FabricLoader.getInstance().isModLoaded("iris");
        String loader = FabricLoader.getInstance().isModLoaded("quilt_loader") ? "Quilt" : "Fabric";
        log("loader=" + loader + " variant=" + variant + " iris=" + iris + " nuit=" + FabricLoader.getInstance().isModLoaded("nuit")
            + " modmenu=" + FabricLoader.getInstance().isModLoaded("modmenu"));
        try {
            if (variant.startsWith("gallery")) {
                gallery(context, variant, iris);
            } else {
                checks(context, variant, iris);
            }
        } finally {
            SunOrientationConfig.set(SunOrientationConfig.DEFAULT_SUNRISE_BEARING, true);
            SunOrientationConfig.save();
            report.write();
        }
        if (!report.failures().isEmpty()) {
            throw new AssertionError("SunOrientation: " + report.failures().size() + " failed check(s): " + report.failures());
        }
    }

    // ------------------------------------------------------------------ checks

    private void checks(ClientGameTestContext context, String variant, boolean iris) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            hideHud(context);
            world.getServer().runCommand("weather clear 100000");
            world.getServer().runCommand("execute as @p at @p run fill ~5 ~ ~-5 ~5 ~10 ~-5 minecraft:stone");
            context.waitTicks(iris ? 400 : 60);

            for (int bearing : GameTestReport.BEARINGS) {
                setBearing(context, bearing, iris);
                for (int time : GameTestReport.TIMES) {
                    world.getServer().runCommand("time set " + time);
                    context.waitTicks(20);
                    if (iris) {
                        context.runOnClient(mc -> report.checkIris(bearing, time));
                    }
                }
                // Screenshot facing the configured sunrise (Minecraft yaw = bearing - 180).
                world.getServer().runCommand("time set 0");
                view(context, world, bearing - 180, -12);
                context.takeScreenshot(variant + "-bearing" + bearing + "-sunrise");
                world.getServer().runCommand("time set 12000");
                view(context, world, bearing, -12);
                context.takeScreenshot(variant + "-bearing" + bearing + "-sunset");
            }

            report.checkHooks(iris);

            checkConfigScreen(context, variant);
            checkModMenu();
        }
    }

    private void setBearing(ClientGameTestContext context, int bearing, boolean iris) {
        context.runOnClient(mc -> {
            SunOrientationConfig.set(bearing, true);
            if (iris) {
                IrisCompat.reloadShaderPack();
            }
        });
        context.waitTicks(iris ? 200 : 10);
        log("sunrise bearing = " + bearing + "°");
    }

    private void checkConfigScreen(ClientGameTestContext context, String variant) {
        context.runOnClient(mc -> SunOrientationConfig.set(180, true));
        context.setScreen(() -> new SunOrientationScreen(null));
        context.waitTicks(5);
        context.takeScreenshot(variant + "-config");

        context.clickScreenButton("sun_orientation.letter.north");
        context.waitTicks(2);
        expect(SunOrientationConfig.get().sunriseBearing() == 0, "N button: live preview at 0°");
        context.clickScreenButton("gui.cancel");
        context.waitTicks(2);
        expect(SunOrientationConfig.get().sunriseBearing() == 180, "Cancel reverts the preview");

        context.setScreen(() -> new SunOrientationScreen(null));
        context.waitTicks(2);
        context.clickScreenButton("sun_orientation.letter.west");
        context.clickScreenButton("gui.done");
        context.waitTicks(60);
        String saved = GameTestReport.readConfigFile();
        expect(SunOrientationConfig.get().sunriseBearing() == 270 && saved.contains("\"sunriseBearing\": 270"),
            "Done saves 270° to the file: " + saved.replaceAll("\\s+", " "));

        // Spanish.
        CompletableFuture<Void> reload = context.computeOnClient(mc -> {
            mc.getLanguageManager().setSelected("es_es");
            mc.options.languageCode = "es_es";
            return mc.reloadResourcePacks();
        });
        context.waitFor(mc -> reload.isDone() && GameTestCompat.overlayGone(mc), 1200);
        context.setScreen(() -> new SunOrientationScreen(null));
        context.waitTicks(5);
        context.takeScreenshot(variant + "-config-es");
        context.setScreen(() -> null);
    }

    private void checkModMenu() {
        if (!FabricLoader.getInstance().isModLoaded("modmenu")) {
            return;
        }
        boolean registered = FabricLoader.getInstance().getEntrypointContainers("modmenu", Object.class).stream()
            .anyMatch(c -> c.getProvider().getMetadata().getId().equals(SunOrientation.MOD_ID));
        expect(registered, "Mod Menu entrypoint registered");
    }

    // ------------------------------------------------------------------ gallery

    private void gallery(ClientGameTestContext context, String variant, boolean iris) {
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(this::normalWorld)
                .create()) {
            world.getConnection().waitForChunksRender();
            hideHud(context);
            world.getServer().runCommand("weather clear 100000");
            world.getServer().runCommand("gamemode spectator @p");
            context.runOnClient(mc -> mc.options.renderDistance().set(12));
            BlockPos spot = world.getServer().computeOnServer(server -> findScenicSpot(server.overworld()));
            log("gallery spot: " + spot);
            world.getServer().runCommand("tp @p " + spot.getX() + " 200 " + spot.getZ());
            context.waitTicks(40);
            world.getServer().runCommand("execute as @p at @p positioned over motion_blocking run tp @s ~ ~"
                + System.getProperty("sunorientation.height", "4") + " ~");
            context.waitTicks(iris ? 900 : 400);
            world.getConnection().waitForChunksRender(false, 2400);
            context.runOnClient(mc -> SunOrientationConfig.set(180, true));
            if (iris) {
                context.runOnClient(mc -> IrisCompat.reloadShaderPack());
                context.waitTicks(200);
            }

            shot(context, world, variant + "-sunrise-south", 300, 0, -4);
            shot(context, world, variant + "-morning", 2500, 0, 35);
            shot(context, world, variant + "-sunset-north", 11000, 180, 6);
            shot(context, world, variant + "-night-moon-south", 16000, 0, -35);

            world.getServer().runCommand("time set 1500");
            view(context, world, 0, 0);
            context.waitTicks(40);
            context.setScreen(() -> new SunOrientationScreen(null));
            context.waitTicks(10);
            context.takeScreenshot(variant + "-config");
            context.setScreen(() -> null);
        }
    }

    /** Looks near the origin for a biome with nice views; falls back to the spawn point. */
    private static BlockPos findScenicSpot(ServerLevel level) {
        List<ResourceKey<Biome>> wanted = List.of(Biomes.CHERRY_GROVE, Biomes.MEADOW, Biomes.SUNFLOWER_PLAINS, Biomes.PLAINS);
        for (ResourceKey<Biome> key : wanted) {
            Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(h -> h.is(key), BlockPos.ZERO, 3200, 32, 64);
            if (found != null) {
                return found.getFirst();
            }
        }
        return BlockPos.ZERO;
    }
    private void normalWorld(WorldCreationUiState creator) {
        Holder<WorldPreset> normal = creator.getSettings().worldgenLoadContext()
            .lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL);
        creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(normal));
        creator.setSeed(System.getProperty("sunorientation.seed", "sunorientation"));
    }

    private void shot(ClientGameTestContext context, TestSingleplayerContext world, String name, int time, int yaw, int pitch) {
        world.getServer().runCommand("time set " + time);
        view(context, world, yaw, pitch);
        context.waitTicks(30);
        context.takeScreenshot(TestScreenshotOptions.of(name).withSize(1920, 1080));
    }

    // ------------------------------------------------------------------ utilities

    private static void hideHud(ClientGameTestContext context) {
        context.runOnClient(GameTestCompat::hideHud);
    }

    private static void view(ClientGameTestContext context, TestSingleplayerContext world, int yaw, int pitch) {
        world.getServer().runCommand("execute as @p at @p run tp @s ~ ~ ~ " + yaw + " " + pitch);
        context.waitTicks(8);
    }

    private void log(String line) {
        report.log(line);
    }

    private void expect(boolean condition, String what) {
        report.expect(condition, what);
    }
}
//?}
