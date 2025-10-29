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
        int blended = ColorMath.blend(baseColor, 0x000000, 0.6f);
        if ((blended & 0xFFFFFF) == 0 && (baseColor & 0xFFFFFF) != 0) {
            return ColorMath.scaleBrightness(baseColor, 0.35f);
        }
        return blended;
    }
}
