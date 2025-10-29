package kamkeel.hextext.client.render.font;

import kamkeel.hextext.common.util.ColorMath;

/**
 * Utilities for HexText's glowing text outline rendering.
 */
public final class GlowingTextRenderer {

    private static final float[][] OUTLINE_OFFSETS = new float[][] {
        {-1.0f, 0.0f},
        {1.0f, 0.0f},
        {0.0f, -1.0f},
        {0.0f, 1.0f},
        {-1.0f, -1.0f},
        {-1.0f, 1.0f},
        {1.0f, -1.0f},
        {1.0f, 1.0f}
    };

    private static final float OUTLINE_DARKEN_FACTOR = 0.4f;
    private static final float OUTLINE_RECOVERY_FACTOR = 0.35f;

    private static boolean outlineEnabled = true;

    private GlowingTextRenderer() {
    }

    public static boolean isOutlineEnabled() {
        return outlineEnabled;
    }

    public static void setOutlineEnabled(boolean enabled) {
        outlineEnabled = enabled;
    }

    public static float[][] getOutlineOffsets() {
        return OUTLINE_OFFSETS;
    }

    public static int computeOutlineColor(int baseColor) {
        int maskedBase = baseColor & 0xFFFFFF;
        if (maskedBase == 0) {
            return 0xFFFFFF;
        }

        int darkened = ColorMath.scaleBrightness(maskedBase, OUTLINE_DARKEN_FACTOR);
        if ((darkened & 0xFFFFFF) == 0) {
            return ColorMath.scaleBrightness(maskedBase, OUTLINE_RECOVERY_FACTOR);
        }
        return darkened;
    }
}
