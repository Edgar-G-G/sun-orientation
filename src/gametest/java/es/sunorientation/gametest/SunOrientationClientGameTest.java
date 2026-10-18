package es.sunorientation.gametest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.mojang.blaze3d.vertex.PoseStack;
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
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * In-game test (development only). Variants ({@code -Psunorientation.variant=...}):
 * <ul>
 *   <li>{@code vanilla}, {@code iris}, {@code nuit}, {@code iris-nuit}: flat world with a pillar; for
 *       several sunrise bearings it checks with the real code (and the already patched Iris) that the light
 *       rises there and sets on the opposite side, that shadows look from the light and that the hooks
 *       run; it also tests the configuration screen (buttons, Cancel, Done) and the Spanish translation;</li>
 *   <li>{@code gallery}, {@code gallery-vanilla}: normal world and 1920x1080 screenshots for the mod page.</li>
 * </ul>
 * The report is written to {@code sunorientation-results.txt}.
 */
public class SunOrientationClientGameTest implements FabricClientGameTest {
    private static final float EPS = 0.02F;
    private static final int[] BEARINGS = {180, 0, 270, 45};

    private final List<String> report = new ArrayList<>();
    private final List<String> failures = new ArrayList<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        String variant = System.getProperty("sunorientation.variant", "vanilla");
        boolean iris = FabricLoader.getInstance().isModLoaded("iris");
        log("variant=" + variant + " iris=" + iris + " nuit=" + FabricLoader.getInstance().isModLoaded("nuit")
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
            writeReport();
        }
        if (!failures.isEmpty()) {
            throw new AssertionError("SunOrientation: " + failures.size() + " failed check(s): " + failures);
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

            for (int bearing : BEARINGS) {
                setBearing(context, bearing, iris);
                for (int time : new int[] {0, 6000, 12000, 14000}) {
                    world.getServer().runCommand("time set " + time);
                    context.waitTicks(20);
                    if (iris) {
                        int expected = time == 6000 ? -1 : time == 12000 ? bearing + 180 : bearing;
                        context.runOnClient(mc -> checkIris(bearing, time, expected));
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

            Set<String> hooks = SunOrientation.activeHooks();
            log("active hooks: " + hooks);
            expectHook(hooks, SunOrientation.Hooks.CELESTIAL, !iris);
            expectHook(hooks, SunOrientation.Hooks.SUNRISE_GLOW, !iris);
            expectHook(hooks, SunOrientation.Hooks.FOG, !iris);
            if (iris) {
                expectHook(hooks, SunOrientation.Hooks.IRIS_UNIFORMS, true);
                expectHook(hooks, SunOrientation.Hooks.IRIS_SHADOWS, true);
                expectHook(hooks, SunOrientation.Hooks.SHADER_PATCH, true);
            }

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

    /** Calls the real Iris code (with this mod's mixins applied) through reflection. */
    private void checkIris(int bearing, int time, int expectedBearing) {
        try {
            Class<?> cu = Class.forName("net.irisshaders.iris.uniforms.CelestialUniforms");
            Object uniforms = cu.getConstructor(float.class).newInstance(0.0F);
            Vector4f light4 = (Vector4f) cu.getMethod("getShadowLightPositionInWorldSpace").invoke(uniforms);
            Vector3f light = new Vector3f(light4.x, light4.y, light4.z).normalize();
            boolean isDay = (boolean) cu.getMethod("isDay").invoke(null);
            float sunAngle = (float) cu.getMethod("getSunAngle", boolean.class).invoke(null, isDay);
            String label = "bearing " + bearing + " t=" + time;
            log(String.format("Iris %s: isDay=%s light=(%.3f, %.3f, %.3f)", label, isDay, light.x, light.y, light.z));

            if (expectedBearing < 0) {
                expect(light.y > 0.95F, label + ": noon at the zenith " + light);
            } else {
                double r = Math.toRadians(expectedBearing);
                Vector3f horizontal = new Vector3f(light.x, 0, light.z).normalize();
                Vector3f want = new Vector3f((float) Math.sin(r), 0, (float) -Math.cos(r));
                expect(horizontal.distance(want) < 0.05F, label + ": light towards " + expectedBearing + "° " + horizontal);
            }

            Vector4f view4 = (Vector4f) cu.getMethod("getShadowLightPosition").invoke(uniforms);
            Class<?> crs = Class.forName("net.irisshaders.iris.uniforms.CapturedRenderingState");
            Object state = crs.getField("INSTANCE").get(null);
            Matrix4f gbuffer = new Matrix4f((Matrix4fc) crs.getMethod("getGbufferModelView").invoke(state));
            Vector3f viewToWorld = gbuffer.invert().transformDirection(new Vector3f(view4.x, view4.y, view4.z)).normalize();
            expect(viewToWorld.distance(light) < EPS, label + ": view position = world position " + viewToWorld);

            Class<?> sm = Class.forName("net.irisshaders.iris.shadows.ShadowMatrices");
            PoseStack pose = new PoseStack();
            sm.getMethod("createBaselineModelViewMatrix", PoseStack.class, float.class, float.class, float.class, float.class)
                .invoke(null, pose, sunAngle / 360.0F, 0.0F, -100.05F, 156.0F);
            Vector3f inShadowView = pose.last().pose().transformDirection(new Vector3f(light));
            expect(Math.abs(inShadowView.x) < EPS && Math.abs(inShadowView.y) < EPS && inShadowView.z > 1 - EPS,
                label + ": the shadow camera looks from the light " + inShadowView);
        } catch (ReflectiveOperationException e) {
            expect(false, "could not call Iris: " + e);
        }
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
        String saved = readConfigFile();
        expect(SunOrientationConfig.get().sunriseBearing() == 270 && saved.contains("\"sunriseBearing\": 270"),
            "Done saves 270° to the file: " + saved.replaceAll("\\s+", " "));

        // Spanish.
        CompletableFuture<Void> reload = context.computeOnClient(mc -> {
            mc.getLanguageManager().setSelected("es_es");
            mc.options.languageCode = "es_es";
            return mc.reloadResourcePacks();
        });
        context.waitFor(mc -> reload.isDone() && mc.gui.overlay() == null, 1200);
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
        context.runOnClient(mc -> {
            if (!mc.gui.hud.isHidden()) {
                mc.gui.hud.toggle();
            }
        });
    }

    private static void view(ClientGameTestContext context, TestSingleplayerContext world, int yaw, int pitch) {
        world.getServer().runCommand("execute as @p at @p run tp @s ~ ~ ~ " + yaw + " " + pitch);
        context.waitTicks(8);
    }

    private static String readConfigFile() {
        try {
            return Files.readString(SunOrientationConfig.path());
        } catch (IOException e) {
            return "(could not read: " + e + ")";
        }
    }

    private void expectHook(Set<String> hooks, String hook, boolean required) {
        if (hooks.contains(hook)) {
            log("OK hook: " + hook);
        } else if (required) {
            expect(false, "hook did not run: " + hook);
        } else {
            log("(hook did not run, expected with shaders: " + hook + ")");
        }
    }

    private void expect(boolean condition, String what) {
        if (condition) {
            log("OK " + what);
        } else {
            failures.add(what);
            log("FAILED " + what);
        }
    }

    private void log(String line) {
        report.add(line);
        SunOrientation.LOGGER.info("[SunOrientation-Test] {}", line);
    }

    private void writeReport() {
        report.add(failures.isEmpty() ? "RESULT: OK" : "RESULT: " + failures.size() + " FAILURE(S)");
        try {
            Files.write(Path.of("sunorientation-results.txt"), report);
        } catch (IOException e) {
            SunOrientation.LOGGER.warn("[SunOrientation-Test] could not write the report", e);
        }
    }
}
