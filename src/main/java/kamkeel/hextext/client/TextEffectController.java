package kamkeel.hextext.client;

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
    private static final float RAINBOW_SPEED = 55.0f;
    private static final float SHAKE_RANGE = 1.2f;
    private static final float IGNITE_FACTOR = 0.35f;

    private final Random random = new Random();

    private boolean rainbowActive;
    private boolean dinnerboneActive;
    private boolean igniteActive;
    private boolean shakeActive;
    private int rainbowAnchorIndex;
    private long rainbowStartTime;
    private long igniteStartTime;
    private int baseColor;
    private boolean transformApplied;

    public void begin(int initialColor) {
        baseColor = initialColor;
        rainbowActive = false;
        dinnerboneActive = false;
        igniteActive = false;
        shakeActive = false;
        rainbowAnchorIndex = 0;
        rainbowStartTime = Minecraft.getSystemTime();
        igniteStartTime = rainbowStartTime;
        transformApplied = false;
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
    }

    public void setRainbow(boolean enabled, int anchorIndex) {
        if (enabled) {
            rainbowActive = true;
            rainbowAnchorIndex = anchorIndex;
            rainbowStartTime = Minecraft.getSystemTime();
        } else {
            rainbowActive = false;
        }
    }

    public void setDinnerbone(boolean enabled) {
        dinnerboneActive = enabled;
    }

    public void setIgnite(boolean enabled) {
        if (enabled && !igniteActive) {
            igniteStartTime = Minecraft.getSystemTime();
        }
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

        if (rainbowActive) {
            long now = Minecraft.getSystemTime();
            float timeShift = (now - rainbowStartTime) / RAINBOW_SPEED;
            float hueDegrees = (charIndex - rainbowAnchorIndex) * RAINBOW_SPREAD + timeShift * 360.0f;
            color = ColorCodeUtils.hsvToRgb(hueDegrees, 1.0f, 1.0f);
        }

        if (igniteActive) {
            long elapsed = Minecraft.getSystemTime() - igniteStartTime;
            boolean brightPhase = ((elapsed / 120L) & 1L) == 0L;
            if (!brightPhase) {
                color = darken(color, IGNITE_FACTOR);
            }
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

        if (shakeActive) {
            applyShake(charIndex);
        }

        if (dinnerboneActive) {
            float width = Math.max(1.0f, fontRenderer.getCharWidth(glyph));
            float pivotX = posX + width * 0.5f;
            float pivotY = posY + fontHeight * 0.5f;
            GL11.glTranslatef(pivotX, pivotY, 0.0f);
            GL11.glScalef(1.0f, -1.0f, 1.0f);
            GL11.glTranslatef(-pivotX, -pivotY, 0.0f);
        }
    }

    public void afterGlyph() {
        if (transformApplied) {
            GL11.glPopMatrix();
            transformApplied = false;
        }
    }

    private void applyShake(int charIndex) {
        long now = Minecraft.getSystemTime();
        long seed = ((long) charIndex * 341873128712L) ^ (now / 16L);
        random.setSeed(seed);
        float offsetX = (random.nextFloat() - 0.5f) * SHAKE_RANGE;
        float offsetY = (random.nextFloat() - 0.5f) * SHAKE_RANGE;
        GL11.glTranslatef(offsetX, offsetY, 0.0f);
    }

    private static int darken(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return (r << 16) | (g << 8) | b;
    }
}
