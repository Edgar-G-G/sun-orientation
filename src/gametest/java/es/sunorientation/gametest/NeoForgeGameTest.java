package es.sunorientation.gametest;

//? if neoforge {
/*import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

import es.sunorientation.IrisCompat;
import es.sunorientation.SunOrientation;
import es.sunorientation.config.SunOrientationConfig;
import es.sunorientation.config.SunOrientationScreen;
import es.sunorientation.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import static es.sunorientation.gametest.GameTestCompat.*;

// In-game test for NeoForge (development only; NeoForge has no client gametest like Fabric's).
// Run with: gradlew :<version>-neoforge:runClientGameTest -Psunorientation.variant=vanilla|iris
// It creates the vanilla flat test world, runs the same checks as the Fabric test (GameTestReport), takes
// screenshots, writes sunorientation-results.txt and closes the game. Every step runs on the client thread,
// one per tick, after the given number of ticks.
@Mod(value = "sun_orientation_gametest", dist = Dist.CLIENT)
public final class NeoForgeGameTest {
    private static final int TIMEOUT_TICKS = 6000;

    private final GameTestReport report = new GameTestReport();
    private final Deque<Step> steps = new ArrayDeque<>();
    private final String variant = System.getProperty("sunorientation.variant", "vanilla");
    private final boolean iris = Platform.isModLoaded("iris");
    private int wait;
    private int waited;
    private boolean started;

    private record Step(int ticks, BooleanSupplier until, Runnable action) {
    }

    public NeoForgeGameTest() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    private void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            // First screen after loading: the title screen, or the accessibility screen of a new game folder.
            if (screen() != null && overlayGone(mc)) {
                started = true;
                plan();
            }
            return;
        }
        if (steps.isEmpty()) {
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = steps.peek();
        if (!step.until().getAsBoolean()) {
            if (++waited % 200 == 0) {
                Screen screen = screen();
                SunOrientation.LOGGER.info("[SunOrientation-Test] waiting ({} ticks), screen: {}", waited,
                    screen == null ? "none" : screen.getClass().getName());
            }
            if (waited > TIMEOUT_TICKS) {
                report.expect(false, "timed out waiting for a step");
                finish();
            }
            return;
        }
        steps.poll();
        waited = 0;
        try {
            step.action().run();
        } catch (RuntimeException e) {
            report.expect(false, "step failed: " + e);
            SunOrientation.LOGGER.error("[SunOrientation-Test] step failed", e);
            finish();
            return;
        }
        if (!steps.isEmpty()) {
            wait = steps.peek().ticks();
        }
    }

    private void then(int ticks, Runnable action) {
        steps.add(new Step(ticks, () -> true, action));
    }

    private void thenWhen(BooleanSupplier until, Runnable action) {
        steps.add(new Step(0, until, action));
    }

    private void plan() {
        Minecraft mc = Minecraft.getInstance();
        report.log("loader=" + Platform.loader() + " variant=" + variant + " iris=" + iris
            + " nuit=" + Platform.isModLoaded("nuit"));

        modsList();

        // Vanilla flat test world: creative, no time or weather change, no mobs.
        then(0, () -> CreateWorldScreen.testWorld(mc, () -> setScreen(new TitleScreen())));
        thenWhen(() -> screen() instanceof CreateWorldScreen, () -> invoke(screen(), "onCreate"));
        thenWhen(() -> {
            // The test world enables the experimental "tests" data pack: accept the warning.
            if (screen() instanceof net.minecraft.client.gui.screens.ConfirmScreen confirm) {
                confirmScreenCallback(confirm).accept(true);
            }
            return mc.level != null && mc.player != null && screen() == null;
        }, () -> {
            hideHud(mc);
            command("weather clear 100000");
            command("execute as @p at @p run fill ~5 ~ ~-5 ~5 ~10 ~-5 minecraft:stone");
        });
        then(iris ? 400 : 100, () -> { });

        for (int bearing : GameTestReport.BEARINGS) {
            then(0, () -> {
                SunOrientationConfig.set(bearing, true);
                if (iris) {
                    IrisCompat.reloadShaderPack();
                }
            });
            then(iris ? 200 : 10, () -> report.log("sunrise bearing = " + bearing + "°"));
            for (int time : GameTestReport.TIMES) {
                then(0, () -> command("time set " + time));
                then(20, () -> {
                    if (iris) {
                        report.checkIris(bearing, time);
                    }
                });
            }
            // Screenshots facing the configured sunrise and sunset (Minecraft yaw = bearing - 180).
            then(0, () -> {
                command("time set 0");
                command("execute as @p at @p run tp @s ~ ~ ~ " + (bearing - 180) + " -12");
            });
            then(10, () -> screenshot(variant + "-bearing" + bearing + "-sunrise"));
            then(0, () -> {
                command("time set 12000");
                command("execute as @p at @p run tp @s ~ ~ ~ " + bearing + " -12");
            });
            then(10, () -> screenshot(variant + "-bearing" + bearing + "-sunset"));
        }

        then(0, () -> report.checkHooks(iris));
        configScreen(mc);
        then(0, () -> {
            boolean registered = ModList.get().getModContainerById(SunOrientation.MOD_ID)
                .flatMap(mod -> mod.getCustomExtension(IConfigScreenFactory.class)).isPresent();
            report.expect(registered, "NeoForge mods list configuration screen registered");
        });
        then(0, this::finish);
    }

    // As a player would: title screen → Mods → search "Sun Orientation" → select it → Config.
    private void modsList() {
        then(0, () -> setScreen(new TitleScreen()));
        then(5, () -> press("fml.menu.mods"));
        then(10, () -> {
            screenshot(variant + "-modlist");
            EditBox search = find(EditBox.class, box -> true);
            if (search == null) {
                report.expect(false, "mods list search box found");
                return;
            }
            search.setValue("Sun Orientation");
        });
        then(5, () -> {
            @SuppressWarnings("rawtypes")
            AbstractSelectionList list = find(AbstractSelectionList.class, l -> true);
            java.util.List<?> entries = list == null ? java.util.List.of() : (java.util.List<?>) list.children();
            report.log("mods list entries after the search: " + entries.size());
            // The entry text is drawn, not a widget: match its narration, or take the only result left.
            Object entry = entries.stream().<Object>map(e -> e)
                .filter(e -> narration(e).contains("Sun Orientation") && !narration(e).contains("test"))
                .findFirst().orElse(entries.size() == 1 ? entries.get(0) : null);
            if (entry == null) {
                report.expect(false, "Sun Orientation found in the mods list");
                return;
            }
            selectEntry(list, entry);
        });
        then(5, () -> {
            screenshot(variant + "-modlist-selected");
            press("neoforge.screen.mods.button.config", "fml.menu.mods.config");
        });
        then(5, () -> {
            report.expect(screen() instanceof SunOrientationScreen, "Mods list → Config opens the Sun Orientation screen"
                + " (" + (screen() == null ? "no screen" : screen().getClass().getSimpleName()) + ")");
            screenshot(variant + "-config-from-modlist");
            if (screen() instanceof SunOrientationScreen) {
                press("gui.cancel");
            }
        });
        then(5, () -> report.expect(screen() != null && !(screen() instanceof SunOrientationScreen)
            && !(screen() instanceof TitleScreen), "Cancel returns to the mods list"));
        then(0, () -> setScreen(new TitleScreen()));
    }

    // Selects a list entry (what a click on it does).
    // NeoForge 26.1 keeps the selection in the screen (ModListScreen.setSelected), 26.2+ in the list; a click calls
    // the screen's method when there is one. AbstractSelectionList.Entry is protected, hence reflection.
    @SuppressWarnings("rawtypes")
    private static void selectEntry(AbstractSelectionList list, Object entry) {
        if (!invokeSetSelected(screen(), entry) && !invokeSetSelected(list, entry)) {
            throw new IllegalStateException("no setSelected for " + entry.getClass().getName());
        }
    }

    private static boolean invokeSetSelected(Object target, Object entry) {
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            for (var m : c.getDeclaredMethods()) {
                if (m.getName().equals("setSelected") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isInstance(entry)) {
                    try {
                        m.setAccessible(true);
                        m.invoke(target, entry);
                        return true;
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException("could not select the entry", e);
                    }
                }
            }
        }
        return false;
    }

    private static String narration(Object entry) {
        if (entry instanceof net.minecraft.client.gui.components.ObjectSelectionList.Entry<?> e) {
            return e.getNarration().getString();
        }
        return String.valueOf(entry);
    }

    private void configScreen(Minecraft mc) {
        then(0, () -> {
            SunOrientationConfig.set(180, true);
            setScreen(new SunOrientationScreen(null));
        });
        then(5, () -> screenshot(variant + "-config"));
        then(0, () -> press("sun_orientation.letter.north"));
        then(2, () -> report.expect(SunOrientationConfig.get().sunriseBearing() == 0, "N button: live preview at 0°"));
        then(0, () -> press("gui.cancel"));
        then(2, () -> report.expect(SunOrientationConfig.get().sunriseBearing() == 180, "Cancel reverts the preview"));
        then(0, () -> setScreen(new SunOrientationScreen(null)));
        then(2, () -> {
            press("sun_orientation.letter.west");
            press("gui.done");
        });
        then(60, () -> {
            String saved = GameTestReport.readConfigFile();
            report.expect(SunOrientationConfig.get().sunriseBearing() == 270 && saved.contains("\"sunriseBearing\": 270"),
                "Done saves 270° to the file: " + saved.replaceAll("\\s+", " "));
        });

        // Spanish.
        CompletableFuture<?>[] reload = new CompletableFuture<?>[1];
        then(0, () -> {
            mc.getLanguageManager().setSelected("es_es");
            mc.options.languageCode = "es_es";
            reload[0] = mc.reloadResourcePacks();
        });
        thenWhen(() -> reload[0] != null && reload[0].isDone() && overlayGone(mc),
            () -> setScreen(new SunOrientationScreen(null)));
        then(5, () -> screenshot(variant + "-config-es"));
        then(0, () -> setScreen(null));
    }

    private void finish() {
        steps.clear();
        SunOrientationConfig.set(SunOrientationConfig.DEFAULT_SUNRISE_BEARING, true);
        SunOrientationConfig.save();
        report.write();
        Minecraft.getInstance().stop();
    }

    private static void command(String command) {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mainRenderTarget(mc), 1, message -> { });
    }

    // Presses the button of the current screen (also inside panels and lists) whose text is one of these
    // translation keys.
    private void press(String... keys) {
        java.util.Set<String> wanted = java.util.Set.of(keys);
        AbstractButton button = find(AbstractButton.class,
            b -> b.getMessage().getContents() instanceof TranslatableContents t && wanted.contains(t.getKey()));
        if (button == null) {
            report.expect(false, "button not found: " + String.join(" / ", keys));
            return;
        }
        button.onPress(new KeyEvent(0, 0, 0));
    }

    private static <T> T find(Class<T> type, java.util.function.Predicate<T> matches) {
        Screen screen = screen();
        return screen == null ? null : find(screen.children(), type, matches);
    }

    private static <T> T find(java.util.List<? extends GuiEventListener> children, Class<T> type,
                              java.util.function.Predicate<T> matches) {
        for (GuiEventListener child : children) {
            if (type.isInstance(child) && matches.test(type.cast(child))) {
                return type.cast(child);
            }
            if (child instanceof ContainerEventHandler container) {
                T found = find(container.children(), type, matches);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static it.unimi.dsi.fastutil.booleans.BooleanConsumer confirmScreenCallback(
            net.minecraft.client.gui.screens.ConfirmScreen screen) {
        try {
            var field = net.minecraft.client.gui.screens.ConfirmScreen.class.getDeclaredField("callback");
            field.setAccessible(true);
            return (it.unimi.dsi.fastutil.booleans.BooleanConsumer) field.get(screen);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("could not read the confirmation callback", e);
        }
    }

    private static void invoke(Object target, String method) {
        try {
            var m = target.getClass().getDeclaredMethod(method);
            m.setAccessible(true);
            m.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("could not call " + method, e);
        }
    }
}
*///?}
