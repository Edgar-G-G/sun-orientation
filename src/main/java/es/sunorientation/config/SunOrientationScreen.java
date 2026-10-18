package es.sunorientation.config;

import java.util.Optional;

import es.sunorientation.IrisCompat;
import es.sunorientation.SkyRotation;
import es.sunorientation.SunOrientation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Configuration screen: a compass seen from above (north up) with the sun at the sunrise direction and
 * the sunset on the opposite side. Pick it with the N/E/S/W buttons, the slider, or by clicking and dragging
 * on the compass. Changes show immediately in the sky; "Done" saves them and, if a shader pack is active,
 * reloads it so the GLSL patch uses the new angle. "Cancel" or Esc reverts them.
 */
public class SunOrientationScreen extends Screen {
    private static final Identifier COMPASS = id("textures/gui/compass.png");
    private static final Identifier SUN = id("textures/gui/sun.png");
    private static final Identifier SUNSET = id("textures/gui/sunset.png");

    private static final int DIAL = 96;
    private static final int DIAL_RADIUS = 42;
    private static final int MARKER_RADIUS = 31;
    private static final int PRESET = 20;
    private static final int LEFT_WIDTH = DIAL + 2 * (PRESET + 2);
    private static final int RIGHT_WIDTH = 176;
    private static final int GAP = 18;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFFA8A8B8;
    private static final int SUNRISE_COLOR = 0xFFFFD54A;
    private static final int SUNSET_COLOR = 0xFFF0884A;

    private static final String[] DIRECTION_KEYS = {
        "north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"
    };

    private final @Nullable Screen parent;
    private final SunOrientationConfig.State original;
    private int bearing;
    private boolean adaptShaderPacks;

    private int dialX;
    private int dialY;
    private int rightX;
    private int top;
    private boolean draggingDial;
    private @Nullable BearingSlider slider;

    public SunOrientationScreen(@Nullable Screen parent) {
        super(Component.translatable("sun_orientation.config.title"));
        this.parent = parent;
        this.original = SunOrientationConfig.get();
        this.bearing = original.sunriseBearing();
        this.adaptShaderPacks = original.adaptShaderPacks();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SunOrientation.MOD_ID, path);
    }

    @Override
    protected void init() {
        int contentWidth = LEFT_WIDTH + GAP + RIGHT_WIDTH;
        int left = (this.width - contentWidth) / 2;
        int contentHeight = LEFT_WIDTH;
        top = Math.max(44, (this.height - 32 - contentHeight) / 2 + 8);
        dialX = left + PRESET + 2;
        dialY = top + PRESET + 2;
        rightX = left + LEFT_WIDTH + GAP;

        int mid = DIAL / 2 - PRESET / 2;
        addPreset(0, dialX + mid, top);
        addPreset(90, dialX + DIAL + 2, dialY + mid);
        addPreset(180, dialX + mid, dialY + DIAL + 2);
        addPreset(270, left, dialY + mid);

        slider = addRenderableWidget(new BearingSlider(rightX, top + 46, RIGHT_WIDTH, 20));

        CycleButton<Boolean> shaders = CycleButton.onOffBuilder(adaptShaderPacks)
            .withTooltip(value -> Tooltip.create(Component.translatable("sun_orientation.config.shaders.tooltip")))
            .create(rightX, top + 72, RIGHT_WIDTH, 20, Component.translatable("sun_orientation.config.shaders"),
                (button, value) -> {
                    adaptShaderPacks = value;
                    apply();
                });
        shaders.active = IrisCompat.isLoaded();
        addRenderableWidget(shaders);

        int buttonsY = this.height - 28;
        int buttonWidth = 100;
        int startX = this.width / 2 - (3 * buttonWidth + 8) / 2;
        addRenderableWidget(Button.builder(Component.translatable("sun_orientation.config.reset"), b -> {
                setBearing(SunOrientationConfig.DEFAULT_SUNRISE_BEARING, false);
                if (adaptShaderPacks != SunOrientationConfig.DEFAULT_ADAPT_SHADER_PACKS) {
                    adaptShaderPacks = SunOrientationConfig.DEFAULT_ADAPT_SHADER_PACKS;
                    rebuildWidgets();
                }
            })
            .bounds(startX, buttonsY, buttonWidth, 20)
            .tooltip(Tooltip.create(Component.translatable("sun_orientation.config.reset.tooltip")))
            .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
            .bounds(startX + buttonWidth + 4, buttonsY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> done())
            .bounds(startX + 2 * (buttonWidth + 4), buttonsY, buttonWidth, 20).build());
    }

    private void addPreset(int presetBearing, int x, int y) {
        Component direction = directionName(presetBearing);
        addRenderableWidget(Button.builder(
                Component.translatable("sun_orientation.letter." + DIRECTION_KEYS[presetBearing / 45]),
                b -> setBearing(presetBearing, false))
            .bounds(x, y, PRESET, PRESET)
            .tooltip(Tooltip.create(Component.translatable("sun_orientation.config.preset", direction)))
            .build());
    }

    // ------------------------------------------------------------------ state

    private void setBearing(int newBearing, boolean fromSlider) {
        bearing = SkyRotation.normalizeBearing(newBearing);
        if (!fromSlider && slider != null) {
            slider.setBearing(bearing);
        }
        apply();
    }

    private void apply() {
        SunOrientationConfig.set(bearing, adaptShaderPacks);
    }

    private void done() {
        apply();
        SunOrientationConfig.save();
        SunOrientationConfig.State now = SunOrientationConfig.get();
        boolean patchChanged = now.adaptShaderPacks() != original.adaptShaderPacks()
            || (now.adaptShaderPacks() && now.extraYaw() != original.extraYaw());
        if (patchChanged) {
            IrisCompat.reloadShaderPack();
        }
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void onClose() {
        SunOrientationConfig.set(original.sunriseBearing(), original.adaptShaderPacks());
        this.minecraft.gui.setScreen(parent);
    }

    // ------------------------------------------------------------------ mouse on the compass

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && overDial(event.x(), event.y())) {
            draggingDial = true;
            setBearingFromMouse(event);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingDial) {
            setBearingFromMouse(event);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingDial = false;
        return super.mouseReleased(event);
    }

    private boolean overDial(double x, double y) {
        return Math.hypot(x - dialCenterX(), y - dialCenterY()) <= DIAL_RADIUS + 2;
    }

    private void setBearingFromMouse(MouseButtonEvent event) {
        double angle = Math.toDegrees(Math.atan2(event.x() - dialCenterX(), dialCenterY() - event.y()));
        int b = SkyRotation.normalizeBearing((int) Math.round(angle));
        if (!event.hasShiftDown()) {
            int snapped = Math.round(b / 45.0F) * 45;
            if (Math.abs(snapped - b) <= 4) {
                b = SkyRotation.normalizeBearing(snapped); // the main bearings "snap"; Shift disables it
            }
        }
        setBearing(b, false);
    }

    private double dialCenterX() {
        return dialX + DIAL / 2.0;
    }

    private double dialCenterY() {
        return dialY + DIAL / 2.0;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(this.font, this.title, this.width / 2, top - 32, WHITE);
        graphics.centeredText(this.font, Component.translatable("sun_orientation.config.subtitle"), this.width / 2, top - 20, GREY);

        drawDial(graphics);

        Component sunriseLabel = Component.translatable("sun_orientation.config.sunrise");
        Component sunsetLabel = Component.translatable("sun_orientation.config.sunset");
        int valueX = rightX + Math.max(this.font.width(sunriseLabel), this.font.width(sunsetLabel)) + 8;
        graphics.text(this.font, sunriseLabel, rightX, top + 4, SUNRISE_COLOR);
        graphics.text(this.font, directionName(bearing), valueX, top + 4, WHITE);
        graphics.text(this.font, sunsetLabel, rightX, top + 18, SUNSET_COLOR);
        graphics.text(this.font, directionName(bearing + 180), valueX, top + 18, WHITE);
        graphics.textWithWordWrap(this.font, statusText(), rightX, top + 100, RIGHT_WIDTH, GREY);
    }

    private void drawDial(GuiGraphicsExtractor graphics) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, COMPASS, dialX, dialY, 0.0F, 0.0F, DIAL, DIAL, DIAL, DIAL);
        double r = Math.toRadians(bearing);
        double sx = Math.sin(r);
        double sy = -Math.cos(r);
        int cx = (int) dialCenterX();
        int cy = (int) dialCenterY();

        // Sun path seen from above: from sunrise to sunset through the zenith.
        for (int i = -MARKER_RADIUS + 9; i <= MARKER_RADIUS - 9; i += 3) {
            if (Math.abs(i) < 4) {
                continue; // the player, in the center
            }
            double t = (MARKER_RADIUS - i) / (2.0 * MARKER_RADIUS); // 0 next to the sun, 1 next to the sunset
            int color = lerpColor(SUNRISE_COLOR, 0xFFB46AD6, t);
            int px = (int) Math.round(cx + sx * i);
            int py = (int) Math.round(cy + sy * i);
            graphics.fill(px - 1, py - 1, px + 1, py + 1, color); // 2x2 centered on the point
        }

        int sunX = (int) Math.round(cx + sx * MARKER_RADIUS) - 8;
        int sunY = (int) Math.round(cy + sy * MARKER_RADIUS) - 8;
        int setX = (int) Math.round(cx - sx * MARKER_RADIUS) - 8;
        int setY = (int) Math.round(cy - sy * MARKER_RADIUS) - 8;
        graphics.blit(RenderPipelines.GUI_TEXTURED, SUNSET, setX, setY, 0.0F, 0.0F, 16, 16, 16, 16);
        graphics.blit(RenderPipelines.GUI_TEXTURED, SUN, sunX, sunY, 0.0F, 0.0F, 16, 16, 16, 16);
    }

    private Component statusText() {
        if (bearing == SkyRotation.VANILLA_SUNRISE_BEARING) {
            return Component.translatable("sun_orientation.config.status.vanilla_direction");
        }
        if (!IrisCompat.isLoaded()) {
            return Component.translatable("sun_orientation.config.status.no_iris");
        }
        Optional<String> pack = IrisCompat.activeShaderPack();
        if (pack.isEmpty()) {
            return Component.translatable("sun_orientation.config.status.shaders_off");
        }
        return Component.translatable(adaptShaderPacks
            ? "sun_orientation.config.status.shaders_on" : "sun_orientation.config.status.shaders_unpatched",
            pack.get().replaceFirst("(?i)\\.zip$", ""));
    }

    /** "South (180°)" for the 8 main bearings; "167° · near South" for the rest. */
    static Component directionName(int anyBearing) {
        int b = SkyRotation.normalizeBearing(anyBearing);
        int nearest = Math.round(b / 45.0F) % 8;
        Component name = Component.translatable("sun_orientation.direction." + DIRECTION_KEYS[nearest]);
        if (b % 45 == 0) {
            return Component.translatable("sun_orientation.direction.exact", name, b);
        }
        return Component.translatable("sun_orientation.direction.near", b, name);
    }

    private static int lerpColor(int from, int to, double t) {
        int r = (int) Math.round(((from >> 16) & 255) * (1 - t) + ((to >> 16) & 255) * t);
        int g = (int) Math.round(((from >> 8) & 255) * (1 - t) + ((to >> 8) & 255) * t);
        int b = (int) Math.round((from & 255) * (1 - t) + (to & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** 0–359° slider synchronized with the compass. */
    private final class BearingSlider extends AbstractSliderButton {
        BearingSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), bearing / 359.0);
            updateMessage();
        }

        private int bearingFromValue() {
            return (int) Math.round(this.value * 359.0);
        }

        void setBearing(int b) {
            this.value = b / 359.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("sun_orientation.config.slider", directionName(bearingFromValue())));
        }

        @Override
        protected void applyValue() {
            SunOrientationScreen.this.setBearing(bearingFromValue(), true);
        }
    }
}
