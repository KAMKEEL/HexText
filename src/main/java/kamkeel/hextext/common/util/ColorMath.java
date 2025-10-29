package kamkeel.hextext.common.util;

/**
 * Simple colour math helpers shared by the text effect controller and tests.
 */
public final class ColorMath {

    private ColorMath() {
    }

    public static int scaleBrightness(int color, float factor) {
        float clampedFactor = clamp(factor, 0.0f, 8.0f);
        int red = clampChannel(((color >> 16) & 0xFF) * clampedFactor);
        int green = clampChannel(((color >> 8) & 0xFF) * clampedFactor);
        int blue = clampChannel((color & 0xFF) * clampedFactor);
        return (red << 16) | (green << 8) | blue;
    }

    public static int blend(int first, int second, float ratio) {
        float clampedRatio = clamp(ratio, 0.0f, 1.0f);
        float inverse = 1.0f - clampedRatio;

        int red = clampChannel(((first >> 16) & 0xFF) * inverse + ((second >> 16) & 0xFF) * clampedRatio);
        int green = clampChannel(((first >> 8) & 0xFF) * inverse + ((second >> 8) & 0xFF) * clampedRatio);
        int blue = clampChannel((first & 0xFF) * inverse + (second & 0xFF) * clampedRatio);

        return (red << 16) | (green << 8) | blue;
    }

    private static int clampChannel(float value) {
        int rounded = Math.round(value);
        if (rounded < 0) {
            return 0;
        }
        if (rounded > 255) {
            return 255;
        }
        return rounded;
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
