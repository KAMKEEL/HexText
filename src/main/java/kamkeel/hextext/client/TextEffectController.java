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
    private static final float SHAKE_RANGE = 1.2f;
    private static final float IGNITE_MIN_FACTOR = 0.35f;

    private final Random random = new Random();

    private boolean rainbowActive;
    private boolean dinnerboneActive;
    private boolean igniteActive;
    private boolean shakeActive;
    private int rainbowAnchorIndex;
    private long rainbowPhaseOffset;
    private boolean rainbowPhaseInitialized;
    private long ignitePhaseOffset;
    private boolean ignitePhaseInitialized;
    private int baseColor;
    private boolean transformApplied;
    private boolean dinnerboneCullDisabled;

    public void begin(int initialColor) {
        baseColor = initialColor;
        rainbowActive = false;
        dinnerboneActive = false;
        igniteActive = false;
        shakeActive = false;
        transformApplied = false;
        dinnerboneCullDisabled = false;
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
        rainbowPhaseInitialized = false;
        ignitePhaseInitialized = false;
        dinnerboneCullDisabled = false;
    }

    public void setRainbow(boolean enabled, int anchorIndex) {
        if (enabled) {
            rainbowActive = true;
            int previousAnchor = rainbowAnchorIndex;
            rainbowAnchorIndex = anchorIndex;
            if (!rainbowPhaseInitialized || previousAnchor != anchorIndex) {
                rainbowPhaseOffset = Minecraft.getSystemTime();
                rainbowPhaseInitialized = true;
            }
        } else {
            rainbowActive = false;
            rainbowPhaseInitialized = false;
        }
    }

    public void setDinnerbone(boolean enabled) {
        dinnerboneActive = enabled;
    }

    public void setIgnite(boolean enabled) {
        if (enabled) {
            igniteActive = true;
            if (!ignitePhaseInitialized) {
                ignitePhaseOffset = Minecraft.getSystemTime();
                ignitePhaseInitialized = true;
            }
        } else {
            igniteActive = false;
            ignitePhaseInitialized = false;
        }
    }

    public void setShake(boolean enabled) {
        shakeActive = enabled;
    }

    public boolean hasActiveEffects() {
        return rainbowActive || dinnerboneActive || igniteActive || shakeActive;
    }

    public int computeColor(int charIndex) {
        int color = baseColor;

        if (rainbowActive && rainbowPhaseInitialized) {
            long now = Minecraft.getSystemTime();
            float rainbowSpeed = Math.max(1.0f, HexTextConfig.getRainbowSpeed());
            float timeShift = (now - rainbowPhaseOffset) / rainbowSpeed;
            float hueDegrees = (charIndex - rainbowAnchorIndex) * RAINBOW_SPREAD + timeShift * 360.0f;
            color = ColorCodeUtils.hsvToRgb(hueDegrees, 1.0f, 1.0f);
        }

        if (igniteActive && ignitePhaseInitialized) {
            long now = Minecraft.getSystemTime();
            long interval = Math.max(1L, HexTextConfig.getIgniteInterval());
            long total = Math.max(1L, interval * 2L);
            long elapsed = now - ignitePhaseOffset;
            float phase = (elapsed % total) / (float) total;
            float brightness;
            if (phase < 0.5f) {
                float t = phase / 0.5f;
                brightness = 1.0f - (1.0f - IGNITE_MIN_FACTOR) * t;
            } else {
                float t = (phase - 0.5f) / 0.5f;
                brightness = IGNITE_MIN_FACTOR + (1.0f - IGNITE_MIN_FACTOR) * t;
            }
            color = applyBrightness(color, brightness);
        }

        return color;
    }

    public void beforeGlyph(FontRenderer fontRenderer, char glyph, int charIndex, float posX, float posY, int fontHeight) {
        if (!dinnerboneActive && !shakeActive) {
            transformApplied = false;
            dinnerboneCullDisabled = false;
            return;
        }

        GL11.glPushMatrix();
        transformApplied = true;
        dinnerboneCullDisabled = false;

        if (shakeActive) {
            applyShake(charIndex);
        }

        if (dinnerboneActive) {
            if (GL11.glIsEnabled(GL11.GL_CULL_FACE)) {
                GL11.glDisable(GL11.GL_CULL_FACE);
                dinnerboneCullDisabled = true;
            }
            float pivotX = posX;
            float pivotY = posY + fontHeight;
            GL11.glTranslatef(pivotX, pivotY, 0.0f);
            GL11.glScalef(1.0f, -1.0f, 1.0f);
            GL11.glTranslatef(-pivotX, -pivotY, 0.0f);
        }
    }

    public void afterGlyph() {
        if (transformApplied) {
            GL11.glPopMatrix();
            if (dinnerboneCullDisabled) {
                GL11.glEnable(GL11.GL_CULL_FACE);
                dinnerboneCullDisabled = false;
            }
            transformApplied = false;
        }
    }

    private void applyShake(int charIndex) {
        long now = Minecraft.getSystemTime();
        long frameWindow = Math.max(1L, HexTextConfig.getShakeInterval());
        long seed = ((long) charIndex * 341873128712L) ^ (now / frameWindow);
        random.setSeed(seed);
        float offsetX = (random.nextFloat() - 0.5f) * SHAKE_RANGE;
        float offsetY = (random.nextFloat() - 0.5f) * SHAKE_RANGE;
        GL11.glTranslatef(offsetX, offsetY, 0.0f);
    }

    public void clearFormattingEffects() {
        dinnerboneActive = false;
        shakeActive = false;
        igniteActive = false;
        ignitePhaseInitialized = false;
        dinnerboneCullDisabled = false;
    }

    private static int applyBrightness(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return (r << 16) | (g << 8) | b;
    }
}
