package kamkeel.hextext.common.util;

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

    /**
     * How far a glyph rides up or down on the wave, in pixels.
     *
     * <p>A travelling sine: position along the string sets the phase and the clock
     * moves it, so the shape holds still relative to the text and the text appears to
     * move through it. Shake is the other kind of motion here - it is noise, reseeded
     * per frame window, and deliberately has no shape at all.</p>
     *
     * @param speed how long, in milliseconds, one full cycle takes
     */
    public static float computeWaveOffset(long now, int charIndex, long speed, float frequency,
                                          float amplitude) {
        if (amplitude == 0.0f) {
            return 0.0f;
        }
        double cycle = speed <= 0L ? 0.0 : (now % speed) / (double) speed * Math.PI * 2.0;
        return (float) (Math.sin(charIndex * frequency + cycle) * amplitude);
    }

    /**
     * The colour of one glyph along a two-colour gradient.
     *
     * <p>Interpolated in straight RGB rather than through a colour space, which is
     * what Angelica's gradient does and is what makes the two agree. A run of one
     * glyph is the start colour: there is no distance to travel, and dividing by the
     * span would divide by zero.</p>
     *
     * @param span how many visible glyphs the gradient covers
     */
    public static int computeGradientColor(int startRgb, int endRgb, int charIndex, int span) {
        if (span <= 1) {
            return startRgb;
        }
        int clamped = charIndex < 0 ? 0 : Math.min(charIndex, span - 1);
        float t = clamped / (float) (span - 1);

        int r = lerpChannel(startRgb >> 16, endRgb >> 16, t);
        int g = lerpChannel(startRgb >> 8, endRgb >> 8, t);
        int b = lerpChannel(startRgb, endRgb, t);
        return (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int from, int to, float t) {
        int a = from & 0xFF;
        int b = to & 0xFF;
        return Math.round(a + (b - a) * t) & 0xFF;
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
