package kamkeel.hextext.client.render.font;

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
        int rgb = baseColor & 0xFFFFFF;
        if (rgb == 0) {
            return 0x7A7A7A;
        }

        float red = ((rgb >> 16) & 0xFF) / 255.0f;
        float green = ((rgb >> 8) & 0xFF) / 255.0f;
        float blue = (rgb & 0xFF) / 255.0f;

        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float delta = max - min;

        float hue;
        if (delta == 0.0f) {
            hue = 0.0f;
        } else if (max == red) {
            hue = ((green - blue) / delta) % 6.0f;
        } else if (max == green) {
            hue = ((blue - red) / delta) + 2.0f;
        } else {
            hue = ((red - green) / delta) + 4.0f;
        }
        hue /= 6.0f;
        if (hue < 0.0f) {
            hue += 1.0f;
        }

        float saturation = max == 0.0f ? 0.0f : (delta / max);
        float value = max;

        float boostedValue = Math.min(1.0f, Math.max(value, 0.35f) * 1.45f);
        if (boostedValue < value + 0.1f) {
            boostedValue = Math.min(1.0f, value + 0.1f);
        }
        float adjustedSaturation = Math.min(1.0f, saturation * 0.65f + 0.15f);

        return hsbToRgb(hue, adjustedSaturation, boostedValue);
    }

    private static int hsbToRgb(float hue, float saturation, float value) {
        hue = wrapHue(hue);
        saturation = clamp01(saturation);
        value = clamp01(value);

        if (saturation == 0.0f) {
            int channel = Math.round(value * 255.0f);
            return (channel << 16) | (channel << 8) | channel;
        }

        float sector = hue * 6.0f;
        int sectorIndex = (int) Math.floor(sector);
        float fraction = sector - sectorIndex;

        float p = value * (1.0f - saturation);
        float q = value * (1.0f - saturation * fraction);
        float t = value * (1.0f - saturation * (1.0f - fraction));

        float r;
        float g;
        float b;
        switch (sectorIndex % 6) {
            case 0:
                r = value;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = value;
                b = p;
                break;
            case 2:
                r = p;
                g = value;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = value;
                break;
            case 4:
                r = t;
                g = p;
                b = value;
                break;
            default:
                r = value;
                g = p;
                b = q;
                break;
        }

        int red = toChannel(r * 255.0f);
        int green = toChannel(g * 255.0f);
        int blue = toChannel(b * 255.0f);
        return (red << 16) | (green << 8) | blue;
    }

    private static float wrapHue(float hue) {
        float wrapped = hue % 1.0f;
        return wrapped < 0.0f ? wrapped + 1.0f : wrapped;
    }

    private static float clamp01(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    private static int toChannel(float value) {
        int rounded = Math.round(value);
        if (rounded < 0) {
            return 0;
        }
        if (rounded > 255) {
            return 255;
        }
        return rounded;
    }
}
