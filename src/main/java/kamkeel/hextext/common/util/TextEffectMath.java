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
     * How far a glyph rides up or down on the wave, in pixels. A travelling sine:
     * position sets the phase and the clock moves it. Shake is the other motion here -
     * noise reseeded per frame window, with no shape at all.
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

    /** Hues of the static rainbow, one step per visible character. */
    private static final int STATIC_RAINBOW_SIZE = 24;
    private static final int[] STATIC_RAINBOW = new int[STATIC_RAINBOW_SIZE];
    static {
        for (int i = 0; i < STATIC_RAINBOW_SIZE; i++) {
            STATIC_RAINBOW[i] = ColorCodeUtils.hsvToRgb(i * 15f, 1f, 1f);
        }
    }

    /**
     * The colour of one glyph of a static rainbow. A fixed table indexed by position, so
     * the text holds still; {@link #computeRainbowColor} is the animated one.
     */
    public static int computeStaticRainbowColor(int charIndex, int anchorIndex) {
        int offset = charIndex - anchorIndex;
        if (offset < 0) {
            offset = 0;
        }
        return STATIC_RAINBOW[offset % STATIC_RAINBOW_SIZE];
    }

    /**
     * The colour of one glyph along a two-colour gradient, interpolated in HSV along the
     * shorter hue arc. Straight RGB fades through greys where turning the hue stays
     * vivid. This is the only ramp HexText draws: under Angelica the translator expands
     * gradients per glyph with it, so both renderers agree. A span of one is the start.
     *
     * @param span how many visible glyphs the gradient covers
     */
    public static int computeGradientColor(int startRgb, int endRgb, int charIndex, int span) {
        if (span <= 1) {
            return startRgb;
        }
        int clamped = charIndex < 0 ? 0 : Math.min(charIndex, span - 1);
        float t = clamped / (float) (span - 1);
        return interpolateHsv(startRgb, endRgb, t);
    }

    private static int interpolateHsv(int startRgb, int endRgb, float t) {
        float[] from = rgbToHsv(startRgb);
        float[] to = rgbToHsv(endRgb);

        // A grey end has no hue of its own; borrowing the other end's keeps a fade
        // to white or black from sweeping through unrelated colours on the way.
        if (from[1] == 0f) {
            from[0] = to[0];
        }
        if (to[1] == 0f) {
            to[0] = from[0];
        }

        float hueDelta = to[0] - from[0];
        if (hueDelta > 180f) {
            hueDelta -= 360f;
        } else if (hueDelta < -180f) {
            hueDelta += 360f;
        }

        float hue = from[0] + hueDelta * t;
        float saturation = from[1] + (to[1] - from[1]) * t;
        float value = from[2] + (to[2] - from[2]) * t;
        return ColorCodeUtils.hsvToRgb(hue, saturation, value);
    }

    private static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float hue = 0f;
        if (delta != 0f) {
            if (max == r) {
                hue = ((g - b) / delta) % 6.0f;
            } else if (max == g) {
                hue = (b - r) / delta + 2.0f;
            } else {
                hue = (r - g) / delta + 4.0f;
            }
            hue *= 60.0f;
            if (hue < 0f) {
                hue += 360.0f;
            }
        }

        float saturation = max == 0f ? 0f : delta / max;
        return new float[] { hue, saturation, max };
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
