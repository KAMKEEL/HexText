package kamkeel.hextext.client.render;

import kamkeel.hextext.common.util.ColorMath;
import kamkeel.hextext.common.util.TextEffectMath;
import kamkeel.hextext.config.HexTextConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

/**
 * Maintains the state for dynamic text effects (rainbow, dinnerbone, ignite and shake).
 */
public final class TextEffectController {

    private static final float RAINBOW_SPREAD = 12.0f;
    private static final float SHAKE_VERTICAL_RANGE = 1.05f;
    /** How far apart in phase two neighbouring glyphs sit on the wave. */
    private static final float WAVE_FREQUENCY = 0.6f;
    private static final float WAVE_AMPLITUDE = 1.5f;
    /** Milliseconds for one full pass of the wave. */
    private static final long WAVE_SPEED = 1000L;
    private static final long SHAKE_Y_SALT = 0xC6A4A7935BD1E995L;
    private static final float IGNITE_MIN_FACTOR = 0.35f;

    private boolean rainbowActive;
    private boolean dinnerboneActive;
    private boolean igniteActive;
    private boolean shakeActive;
    private boolean waveActive;
    private boolean shadowColorActive;
    private int shadowColorOverride;
    private boolean gradientActive;
    private int gradientStartRgb;
    private int gradientEndRgb;
    private int gradientSpan;
    private int gradientAnchorIndex;
    private int rainbowAnchorIndex;
    private boolean rainbowStatic;
    private int baseColor;
    private boolean transformApplied;
    private boolean cullFaceTemporarilyDisabled;

    public void begin(int initialColor) {
        baseColor = initialColor;
        resetDynamicEffects();
        transformApplied = false;
        cullFaceTemporarilyDisabled = false;
    }

    public void updateBaseColor(int color) {
        baseColor = color;
    }

    public int getBaseColor() {
        return baseColor;
    }

    public void resetDynamicEffects() {
        rainbowActive = false;
        dinnerboneActive = false;
        igniteActive = false;
        shakeActive = false;
        waveActive = false;
        gradientActive = false;
        gradientAnchorIndex = 0;
        shadowColorActive = false;
        rainbowAnchorIndex = 0;
        rainbowStatic = false;
    }

    public void setRainbow(boolean enabled, int anchorIndex) {
        setRainbow(enabled, anchorIndex, false);
    }

    /** @param stat_ic a fixed table by position rather than a cycling one */
    public void setRainbow(boolean enabled, int anchorIndex, boolean stat_ic) {
        rainbowActive = enabled;
        rainbowStatic = stat_ic;
        if (enabled) {
            rainbowAnchorIndex = Math.max(0, anchorIndex);
        }
    }

    public void setDinnerbone(boolean enabled) {
        dinnerboneActive = enabled;
    }

    public void setIgnite(boolean enabled) {
        igniteActive = enabled;
    }

    public void setShake(boolean enabled) {
        shakeActive = enabled;
    }

    public void setWave(boolean enabled) {
        waveActive = enabled;
    }

    public void setShadowColor(int rgb, boolean enabled) {
        shadowColorActive = enabled;
        shadowColorOverride = rgb;
    }

    public boolean hasShadowColor() {
        return shadowColorActive;
    }

    public int getShadowColor() {
        return shadowColorOverride;
    }

    /**
     * @param anchorIndex the glyph the gradient starts from, so the ramp is measured
     *                    from where the code was written rather than from the start of
     *                    whatever string it happened to land in
     */
    public void setGradient(int startRgb, int endRgb, int span, int anchorIndex) {
        gradientActive = true;
        gradientStartRgb = startRgb;
        gradientEndRgb = endRgb;
        gradientSpan = Math.max(1, span);
        gradientAnchorIndex = Math.max(0, anchorIndex);
        // A gradient is a colour, and a colour ends the rainbow rather than blending
        // with it - two things driving the same channel would leave neither readable.
        rainbowActive = false;
    }

    public boolean hasActiveEffects() {
        return rainbowActive || dinnerboneActive || igniteActive || shakeActive || waveActive
            || gradientActive || shadowColorActive;
    }

    public int computeColor(int charIndex) {
        int color = baseColor;
        long now = currentTime();

        if (rainbowActive) {
            color = rainbowStatic
                ? TextEffectMath.computeStaticRainbowColor(charIndex, rainbowAnchorIndex)
                : TextEffectMath.computeRainbowColor(now, HexTextConfig.getRainbowSpeed(), charIndex,
                    rainbowAnchorIndex, RAINBOW_SPREAD);
        }

        if (gradientActive) {
            color = TextEffectMath.computeGradientColor(gradientStartRgb, gradientEndRgb,
                charIndex - gradientAnchorIndex, gradientSpan);
        }

        if (igniteActive) {
            float brightness = TextEffectMath.computeIgniteBrightness(now, HexTextConfig.getIgniteInterval(),
                IGNITE_MIN_FACTOR);
            color = ColorMath.scaleBrightness(color, brightness);
        }

        return color;
    }

    public void beforeGlyph(FontRenderer fontRenderer, char glyph, int charIndex, float posX, float posY, int fontHeight) {
        if (!dinnerboneActive && !shakeActive && !waveActive) {
            transformApplied = false;
            return;
        }

        GL11.glPushMatrix();
        transformApplied = true;
        cullFaceTemporarilyDisabled = false;

        if (shakeActive) {
            applyShake(charIndex);
        }

        // After shake, so a glyph carrying both rides the wave and jitters about that
        // point rather than the two fighting over the same translate.
        if (waveActive) {
            GL11.glTranslatef(0.0f, TextEffectMath.computeWaveOffset(currentTime(), charIndex,
                WAVE_SPEED, WAVE_FREQUENCY, WAVE_AMPLITUDE), 0.0f);
        }

        if (dinnerboneActive) {
            if (GL11.glIsEnabled(GL11.GL_CULL_FACE)) {
                GL11.glDisable(GL11.GL_CULL_FACE);
                cullFaceTemporarilyDisabled = true;
            }
            float width = Math.max(1.0f, fontRenderer.getCharWidth(glyph));
            float glyphHeight = Math.max(1.0f, fontHeight - 1.0f);
            float pivotX = posX + width * 0.5f;
            float pivotY = posY + glyphHeight * 0.5f;
            float baselineOffset = Math.max(0.0f, fontHeight - glyphHeight);
            GL11.glTranslatef(pivotX, pivotY, 0.0f);
            GL11.glScalef(1.0f, -1.0f, 1.0f);
            GL11.glTranslatef(-pivotX, -pivotY + baselineOffset, 0.0f);
        }
    }

    public void afterGlyph() {
        if (transformApplied) {
            if (cullFaceTemporarilyDisabled) {
                GL11.glEnable(GL11.GL_CULL_FACE);
                cullFaceTemporarilyDisabled = false;
            }
            GL11.glPopMatrix();
            transformApplied = false;
        }
    }

    private void applyShake(int charIndex) {
        long now = currentTime();
        long frameWindow = Math.max(1L, HexTextConfig.getShakeInterval());
        long seed = TextEffectMath.computeShakeSeed(charIndex, now, frameWindow);
        float offsetY = TextEffectMath.computeShakeOffset(seed ^ SHAKE_Y_SALT, SHAKE_VERTICAL_RANGE);
        GL11.glTranslatef(0.0f, offsetY, 0.0f);
    }

    private long currentTime() {
        return Minecraft.getSystemTime();
    }
}
