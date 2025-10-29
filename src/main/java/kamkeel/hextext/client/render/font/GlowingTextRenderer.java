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

    private static final float LUMINANCE_DARK = 0.20f;
    private static final float LUMINANCE_MID = 0.40f;
    private static final float LUMINANCE_BRIGHT = 0.72f;

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
        int rgb = baseColor & 0xFFFFFF;
        float luminance = computeLuminance(rgb);

        int outline;
        if (luminance <= LUMINANCE_DARK) {
            outline = ColorMath.blend(rgb, 0xFFFFFF, 0.55f);
        } else if (luminance <= LUMINANCE_MID) {
            outline = ColorMath.blend(rgb, 0xFFFFFF, 0.35f);
        } else if (luminance >= LUMINANCE_BRIGHT) {
            outline = ColorMath.blend(rgb, 0x000000, 0.85f);
        } else {
            outline = ColorMath.blend(rgb, 0x000000, 0.6f);
        }

        if ((outline & 0xFFFFFF) == 0 && rgb != 0) {
            return ColorMath.scaleBrightness(rgb, luminance < LUMINANCE_DARK ? 0.75f : 0.35f);
        }

        return outline;
    }

    private static float computeLuminance(int rgb) {
        float red = ((rgb >> 16) & 0xFF) / 255.0f;
        float green = ((rgb >> 8) & 0xFF) / 255.0f;
        float blue = (rgb & 0xFF) / 255.0f;
        return red * 0.2126f + green * 0.7152f + blue * 0.0722f;
    }
}
