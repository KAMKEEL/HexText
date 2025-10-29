package kamkeel.hextext.client;

import kamkeel.hextext.config.HexTextConfig;
import kamkeel.hextext.util.ColorCodeUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * Maintains the state for dynamic text effects (rainbow, dinnerbone, ignite and shake).
 */
public final class TextEffectController {

    private static final float RAINBOW_SPREAD = 12.0f;
    private static final float RAINBOW_SATURATION = 0.92f;
    private static final float RAINBOW_BRIGHTNESS = 0.96f;
    private static final float RAINBOW_LUMINANCE_SOFT = 0.42f;
    private static final float RAINBOW_LUMINANCE_STRONG = 0.26f;
    private static final float RAINBOW_SOFTEN_LIGHT = 0.18f;
    private static final float RAINBOW_SOFTEN_STRONG = 0.32f;
    private static final float SHAKE_RANGE = 1.2f;
    private static final float IGNITE_MIN_FACTOR = 0.35f;

    private final Random random = new Random();

    private boolean rainbowActive;
    private boolean dinnerboneActive;
    private boolean igniteActive;
    private boolean shakeActive;
    private int rainbowAnchorIndex;
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
        rainbowAnchorIndex = 0;
    }

    public void setRainbow(boolean enabled, int anchorIndex) {
        rainbowActive = enabled;
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

    public boolean hasActiveEffects() {
        return rainbowActive || dinnerboneActive || igniteActive || shakeActive;
    }

    public int computeColor(int charIndex) {
        int color = baseColor;

        long now = getCurrentTime();

        if (rainbowActive) {
            double rainbowSpeed = Math.max(1.0, HexTextConfig.getRainbowSpeed());
            double timeDegrees = (now / rainbowSpeed) * 360.0;
            float hueDegrees = (float) ((charIndex - rainbowAnchorIndex) * RAINBOW_SPREAD + timeDegrees);
            color = softenRainbowColor(ColorCodeUtils.hsvToRgb(hueDegrees, RAINBOW_SATURATION, RAINBOW_BRIGHTNESS));
        }

        if (igniteActive) {
            long interval = Math.max(1L, HexTextConfig.getIgniteInterval());
            float brightness = computeIgniteBrightness(now, interval);
            color = scaleBrightness(color, brightness);
        }

        return color;
    }

    public void beforeGlyph(FontRenderer fontRenderer, char glyph, int charIndex, float posX, float posY, int fontHeight) {
        if (!dinnerboneActive && !shakeActive) {
            transformApplied = false;
            return;
        }

        GL11.glPushMatrix();
        transformApplied = true;
        cullFaceTemporarilyDisabled = false;

        if (shakeActive) {
            applyShake(charIndex);
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
        long now = getCurrentTime();
        long frameWindow = Math.max(1L, HexTextConfig.getShakeInterval());
        long seed = ((long) charIndex * 341873128712L) ^ (now / frameWindow);
        random.setSeed(seed);
        float offsetX = (random.nextFloat() - 0.5f) * SHAKE_RANGE;
        float offsetY = (random.nextFloat() - 0.5f) * SHAKE_RANGE;
        GL11.glTranslatef(offsetX, offsetY, 0.0f);
    }

    static float computeIgniteBrightness(long now, long interval) {
        long adjustedInterval = Math.max(1L, interval);
        long period = Math.max(1L, adjustedInterval * 2L);
        long timeInPeriod = now % period;
        float phase = (float) timeInPeriod / (float) period;
        float triangle = Math.abs((phase * 2.0f) - 1.0f);
        return IGNITE_MIN_FACTOR + (1.0f - IGNITE_MIN_FACTOR) * triangle;
    }

    static int softenRainbowColor(int color) {
        float luminance = computeLuminance(color);
        if (luminance <= RAINBOW_LUMINANCE_STRONG) {
            return blendWithWhite(color, RAINBOW_SOFTEN_STRONG);
        }
        if (luminance <= RAINBOW_LUMINANCE_SOFT) {
            return blendWithWhite(color, RAINBOW_SOFTEN_LIGHT);
        }
        return color;
    }

    static int scaleBrightness(int color, float factor) {
        int r = Math.max(0, Math.min(255, Math.round(((color >> 16) & 0xFF) * factor)));
        int g = Math.max(0, Math.min(255, Math.round(((color >> 8) & 0xFF) * factor)));
        int b = Math.max(0, Math.min(255, Math.round((color & 0xFF) * factor)));
        return (r << 16) | (g << 8) | b;
    }

    private static long getCurrentTime() {
        return Minecraft.getSystemTime();
    }

    private static float computeLuminance(int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        return 0.2126f * r + 0.7152f * g + 0.0722f * b;
    }

    private static int blendWithWhite(int color, float mix) {
        float clamped = Math.max(0.0f, Math.min(1.0f, mix));
        int r = (int) (((color >> 16) & 0xFF) * (1.0f - clamped) + 255.0f * clamped);
        int g = (int) (((color >> 8) & 0xFF) * (1.0f - clamped) + 255.0f * clamped);
        int b = (int) ((color & 0xFF) * (1.0f - clamped) + 255.0f * clamped);
        return (r << 16) | (g << 8) | b;
    }
}
