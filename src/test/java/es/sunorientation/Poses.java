package es.sunorientation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * The {@code PoseStack} rotations as each Minecraft version writes them: 26.3 has
 * {@code rotateDegrees(Axis, float)} / {@code rotate(Axis, float)}, older versions
 * {@code mulPose(axis.rotationDegrees(..))}. Both post-multiply the same rotation.
 */
final class Poses {
    private Poses() {
    }

    static void rotateDegrees(PoseStack pose, Axis axis, float degrees) {
        //? if >=26.3 {
        pose.rotateDegrees(axis, degrees);
        //?} else
        //pose.mulPose(axis.rotationDegrees(degrees));
    }

    static void rotate(PoseStack pose, Axis axis, float radians) {
        //? if >=26.3 {
        pose.rotate(axis, radians);
        //?} else
        //pose.mulPose(axis.rotation(radians));
    }
}
