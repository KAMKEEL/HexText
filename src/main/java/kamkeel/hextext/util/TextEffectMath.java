package kamkeel.hextext.util;

/**
 * Deterministic helpers for dynamic text effect calculations.
 */
public final class TextEffectMath {

    private static final float RAINBOW_SATURATION = 0.9f;
    private static final float RAINBOW_LIGHTEN_RATIO = 0.18f;

    private TextEffectMath() {
    }

    public static int computeRainbowColor(long now, double rainbowSpeed, int charIndex, int anchorIndex, float spread) {
        double safeSpeed = rainbowSpeed < 1.0 ? 1.0 : rainbowSpeed;
        double timeDegrees = (now / safeSpeed) * 360.0;
        float hueDegrees = (float) ((charIndex - anchorIndex) * spread + timeDegrees);
        int base = ColorCodeUtils.hsvToRgb(hueDegrees, RAINBOW_SATURATION, 1.0f);
        return ColorMath.blend(base, 0xFFFFFF, RAINBOW_LIGHTEN_RATIO);
    }

    public static float computeIgniteBrightness(long now, long interval, float minimumFactor) {
        long safeInterval = interval <= 0 ? 1L : interval;
        long period = safeInterval * 2L;
        long phase = now % period;
        if (phase < 0) {
            phase += period;
        }

        float progress;
        if (phase <= safeInterval) {
            progress = 1.0f - (float) phase / (float) safeInterval;
        } else {
            progress = (float) (phase - safeInterval) / (float) safeInterval;
        }

        float minClamp = minimumFactor < 0.0f ? 0.0f : (minimumFactor > 1.0f ? 1.0f : minimumFactor);
        return minClamp + (1.0f - minClamp) * progress;
    }

    public static long computeShakeSeed(int charIndex, long now, long frameWindow) {
        long safeFrame = frameWindow <= 0 ? 1L : frameWindow;
        return ((long) charIndex * 341873128712L) ^ (now / safeFrame);
    }

    public static float computeShakeOffset(long seed, float range) {
        float safeRange = range < 0.0f ? 0.0f : range;
        if (safeRange == 0.0f) {
            return 0.0f;
        }
        long mixed = mix(seed);
        float normalized = ((mixed >>> 40) & 0xFFFFFF) / (float) 0xFFFFFF;
        return (normalized * 2.0f - 1.0f) * safeRange;
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }
}
