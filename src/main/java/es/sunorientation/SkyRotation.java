package es.sunorientation;

/**
 * Stateless math of the sky rotation (tested without starting the game).
 *
 * <p>Minecraft axes: east = +X, west = -X, south = +Z, north = -Z. The sunrise direction is a compass
 * bearing: 0 = north, 90 = east (vanilla), 180 = south, 270 = west.
 *
 * <p>The whole mod applies a single rigid rotation around +Y, {@code R = Ry(yaw)}, with the same
 * convention as {@code Matrix4f.rotateY}. Since {@code Ry(yaw)·(1,0,0) = (cos yaw, 0, -sin yaw)} and
 * bearing {@code b} points to {@code (sin b, 0, -cos b)}, the rotation that takes east to bearing
 * {@code b} is {@code yaw = 90 - b}.
 */
public final class SkyRotation {
    /** Vanilla sunrise bearing (east). */
    public static final int VANILLA_SUNRISE_BEARING = 90;

    private SkyRotation() {
    }

    public static int normalizeBearing(int bearing) {
        return Math.floorMod(bearing, 360);
    }

    /** Extra rotation, in degrees around +Y within (-180, 180], that takes east to the given bearing. */
    public static float extraYawForBearing(int sunriseBearing) {
        int yaw = VANILLA_SUNRISE_BEARING - normalizeBearing(sunriseBearing);
        if (yaw <= -180) {
            yaw += 360;
        }
        return yaw;
    }

    /** X component of {@code Ry(yaw)·(x, y, z)}. */
    public static float rotatedX(float x, float z, float sin, float cos) {
        return x * cos + z * sin;
    }

    /** Z component of {@code Ry(yaw)·(x, y, z)}. */
    public static float rotatedZ(float x, float z, float sin, float cos) {
        return -x * sin + z * cos;
    }

    /** Sine and cosine that are exact at multiples of 90° (so cardinal directions carry no error). */
    public static float sinDegrees(float degrees) {
        if (degrees % 90.0F == 0.0F) {
            return switch (Math.floorMod((int) degrees, 360)) {
                case 90 -> 1.0F;
                case 270 -> -1.0F;
                default -> 0.0F;
            };
        }
        return (float) Math.sin(Math.toRadians(degrees));
    }

    public static float cosDegrees(float degrees) {
        return sinDegrees(degrees + 90.0F);
    }
}
