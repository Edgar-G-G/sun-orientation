import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Generates the mod artwork as pixel art (no dependencies):
 *   java tools/GenerateArt.java src/main/resources/assets/sun_orientation publish
 *
 * - icon.png (128x128) and publish/{modrinth,curseforge}/icon-512.png: 32x32 icon scaled without smoothing.
 * - textures/gui/compass.png (96x96): compass dial of the configuration screen.
 * - textures/gui/sun.png, sunset.png (16x16): sunrise and sunset markers.
 */
public class GenerarArte {
    public static void main(String[] args) throws IOException {
        File assets = new File(args[0]);
        File publish = args.length > 1 ? new File(args[1]) : null;
        BufferedImage icon = icon32();
        write(scale(icon, 4), new File(assets, "icon.png"));
        if (publish != null) {
            write(scale(icon, 16), new File(publish, "modrinth/icon-512.png"));
            write(scale(icon, 16), new File(publish, "curseforge/icon-512.png"));
        }
        write(compass(), new File(assets, "textures/gui/compass.png"));
        write(sun(), new File(assets, "textures/gui/sun.png"));
        write(sunset(), new File(assets, "textures/gui/sunset.png"));
    }

    // ---------------------------------------------------------------- icon

    static BufferedImage icon32() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        // Sunrise sky in bands, from night (top) to orange (horizon).
        int[] sky = {0x1B2240, 0x1F2A4E, 0x27335C, 0x33396A, 0x463E74, 0x5E4478, 0x7A4A76, 0x9A5170,
                     0xB95D66, 0xD46F5A, 0xE6864E, 0xF2A04A, 0xF8BC55};
        int horizon = 22;
        for (int y = 0; y < horizon; y++) {
            int band = Math.min(sky.length - 1, y * sky.length / horizon);
            for (int x = 0; x < 32; x++) {
                img.setRGB(x, y, argb(sky[band]));
            }
        }
        // Fading stars.
        int[][] stars = {{5, 3}, {25, 2}, {12, 6}, {28, 8}, {3, 10}, {20, 4}};
        for (int[] s : stars) {
            img.setRGB(s[0], s[1], argb(0xDCE6FF));
        }
        // Rising sun: disc with a halo, cut by the horizon.
        int cx = 16, cy = 22;
        for (int y = 8; y < horizon; y++) {
            for (int x = 0; x < 32; x++) {
                double d = Math.hypot(x + 0.5 - cx, y + 0.5 - cy);
                if (d < 6.2) {
                    img.setRGB(x, y, argb(d < 4.2 ? 0xFFF4B8 : 0xFFD54A));
                } else if (d < 8.5) {
                    img.setRGB(x, y, blend(img.getRGB(x, y), 0xFFB347, 0.55));
                } else if (d < 10.5) {
                    img.setRGB(x, y, blend(img.getRGB(x, y), 0xFF9A3C, 0.25));
                }
            }
        }
        // Rays: the left half is defined and mirrored (x -> 31 - x) around the sun axis.
        int[][] rays = {{15, 10}, {15, 11}, {9, 15}, {8, 14}, {11, 12}};
        for (int[] r : rays) {
            img.setRGB(r[0], r[1], argb(0xFFE07A));
            img.setRGB(31 - r[0], r[1], argb(0xFFE07A));
        }
        // Grass ground.
        for (int y = horizon; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                int c = y == horizon ? 0x6DAA45 : y == horizon + 1 ? 0x4F8A3A : (x * 7 + y * 3) % 5 == 0 ? 0x2B5226 : 0x356530;
                img.setRGB(x, y, argb(c));
            }
        }
        // Compass needle on the ground: red tip pointing at the sun, white tail.
        // Even-width diamond (6 px, columns 13-18) so it is centered on the sun axis (x = 16.0):
        // red half towards the sun, white half, dark outline.
        String[] needle = {
            "..oo..",
            ".orro.",
            ".orro.",
            "orRRro",
            "orRRro",
            "owWWwo",
            "owWWwo",
            ".owwo.",
            ".owwo.",
            "..oo..",
        };
        int[] palette = new int[128];
        palette['o'] = 0x1C1410;
        palette['r'] = 0xE8423A;
        palette['R'] = 0xB52A26;
        palette['w'] = 0xF2F2F2;
        palette['W'] = 0xC9C9C9;
        for (int row = 0; row < needle.length; row++) {
            for (int col = 0; col < needle[row].length(); col++) {
                char ch = needle[row].charAt(col);
                if (ch != '.') {
                    img.setRGB(13 + col, 22 + row, argb(palette[ch]));
                }
            }
        }
        // Rounded (transparent) corners.
        int[][] corners = {{0, 0}, {1, 0}, {0, 1}, {30, 0}, {31, 0}, {31, 1}, {0, 30}, {0, 31}, {1, 31}, {31, 30}, {30, 31}, {31, 31}};
        for (int[] p : corners) {
            img.setRGB(p[0], p[1], 0);
        }
        return img;
    }

    // ---------------------------------------------------------------- compass

    static BufferedImage compass() {
        int size = 96;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        double c = size / 2.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x + 0.5 - c, dy = y + 0.5 - c;
                double d = Math.hypot(dx, dy);
                int col;
                if (d > 47.5) {
                    continue;
                } else if (d > 46.5) {
                    col = 0x2A1C0C; // outer edge
                } else if (d > 43.5) {
                    // brass ring lit from the top left
                    double light = (-dx - dy) / (d * 1.4142);
                    col = light > 0.35 ? 0xF0CF73 : light < -0.35 ? 0x8C6A2C : 0xC9A24A;
                } else if (d > 42.5) {
                    col = 0x3A2A12; // inner edge
                } else {
                    // dial face: night blue with a slight radial gradient
                    double t = d / 42.5;
                    col = lerp(0x2C3D63, 0x18223B, t * t);
                    if (Math.abs(d - 28.5) < 0.5) {
                        col = lerp(col, 0x4A5E8C, 0.6); // guide circle
                    }
                    if (Math.abs(dx) < 0.5 || Math.abs(dy) < 0.5) {
                        col = lerp(col, 0x4A5E8C, 0.45); // north-south / east-west cross
                    }
                }
                img.setRGB(x, y, argb(col));
            }
        }
        // Ticks every 15° (cardinal ones longer), inside the ring.
        for (int deg = 0; deg < 360; deg += 15) {
            boolean cardinal = deg % 90 == 0;
            boolean diagonal = deg % 45 == 0;
            int len = cardinal ? 6 : diagonal ? 4 : 2;
            int col = deg == 0 ? 0xE8423A : cardinal ? 0xF0CF73 : diagonal ? 0xC9A24A : 0x7D8BB0;
            double r = Math.toRadians(deg);
            for (int i = 0; i < len; i++) {
                double rr = 41.5 - i;
                plotSymmetric(img, c + Math.sin(r) * rr, c - Math.cos(r) * rr, argb(col));
            }
        }
        // Center: the player.
        for (int y = 46; y <= 49; y++) {
            for (int x = 46; x <= 49; x++) {
                boolean edge = x == 46 || x == 49 || y == 46 || y == 49;
                boolean corner = (x == 46 || x == 49) && (y == 46 || y == 49);
                if (!corner) {
                    img.setRGB(x, y, argb(edge ? 0x1A1A1A : 0xF2F2F2));
                }
            }
        }
        return img;
    }

    // ---------------------------------------------------------------- markers

    static BufferedImage sun() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                if (d < 3.3) {
                    img.setRGB(x, y, argb(0xFFF6C4));
                } else if (d < 4.6) {
                    img.setRGB(x, y, argb(0xFFD54A));
                } else if (d < 5.4) {
                    img.setRGB(x, y, argb(0xE89A24));
                }
            }
        }
        int[][] rays = {{7, 0}, {8, 0}, {7, 1}, {8, 1}, {7, 14}, {8, 14}, {7, 15}, {8, 15},
                        {0, 7}, {0, 8}, {1, 7}, {1, 8}, {14, 7}, {14, 8}, {15, 7}, {15, 8},
                        {2, 2}, {3, 3}, {13, 2}, {12, 3}, {2, 13}, {3, 12}, {13, 13}, {12, 12}};
        for (int[] r : rays) {
            img.setRGB(r[0], r[1], argb(0xFFC233));
        }
        return img;
    }

    static BufferedImage sunset() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - 7.5, y - 8.5);
                if (y <= 9 && d < 5.4) {
                    img.setRGB(x, y, argb(d < 3.6 ? 0xFFB15C : d < 4.6 ? 0xF07A3A : 0xB8452E));
                }
            }
        }
        for (int x = 1; x <= 14; x++) {
            img.setRGB(x, 10, argb(0x7A3B6E)); // horizon
        }
        for (int x = 4; x <= 11; x++) {
            img.setRGB(x, 12, argb(0x5A2E5E));
        }
        for (int x = 6; x <= 9; x++) {
            img.setRGB(x, 14, argb(0x4A2650));
        }
        return img;
    }

    // ---------------------------------------------------------------- utilities

    /**
     * Paints the pixel containing point (x, y). If the point lies exactly on a boundary between pixels
     * (like the central axis of an even-sized image), both neighbours are painted so the result stays
     * centered and symmetric.
     */
    static void plotSymmetric(BufferedImage img, double x, double y, int color) {
        double ex = Math.abs(x - Math.rint(x)) < 1.0E-6 ? Math.rint(x) : Double.NaN;
        double ey = Math.abs(y - Math.rint(y)) < 1.0E-6 ? Math.rint(y) : Double.NaN;
        int[] xs = Double.isNaN(ex) ? new int[] {(int) Math.floor(x)} : new int[] {(int) ex - 1, (int) ex};
        int[] ys = Double.isNaN(ey) ? new int[] {(int) Math.floor(y)} : new int[] {(int) ey - 1, (int) ey};
        for (int px : xs) {
            for (int py : ys) {
                img.setRGB(px, py, color);
            }
        }
    }

    static int argb(int rgb) {
        return 0xFF000000 | rgb;
    }

    static int lerp(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    static int blend(int argbBase, int rgbTop, double t) {
        return argb(lerp(argbBase & 0xFFFFFF, rgbTop, t));
    }

    static BufferedImage scale(BufferedImage src, int k) {
        BufferedImage out = new BufferedImage(src.getWidth() * k, src.getHeight() * k, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                out.setRGB(x, y, src.getRGB(x / k, y / k));
            }
        }
        return out;
    }

    static void write(BufferedImage img, File file) throws IOException {
        file.getParentFile().mkdirs();
        ImageIO.write(img, "png", file);
        System.out.println("wrote " + file + " (" + img.getWidth() + "x" + img.getHeight() + ")");
    }

    @SuppressWarnings("unused")
    static Color c(int rgb) {
        return new Color(rgb);
    }
}
