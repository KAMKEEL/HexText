package kamkeel.hextext.client;

import kamkeel.hextext.config.HexTextConfig;
import kamkeel.hextext.util.ColorMath;
import kamkeel.hextext.util.TextEffectMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

/**
 * Maintains the state for dynamic text effects (rainbow, dinnerbone, ignite and shake).
 */
public final class TextEffectController {

    private static final float RAINBOW_SPREAD = 12.0f;
    private static final float SHAKE_VERTICAL_RANGE = 1.05f;
    private static final long SHAKE_Y_SALT = 0xC6A4A7935BD1E995L;
    private static final float IGNITE_MIN_FACTOR = 0.35f;

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
        long now = currentTime();

        if (rainbowActive) {
            color = TextEffectMath.computeRainbowColor(now, HexTextConfig.getRainbowSpeed(), charIndex,
                rainbowAnchorIndex, RAINBOW_SPREAD);
        }

        if (igniteActive) {
            float brightness = TextEffectMath.computeIgniteBrightness(now, HexTextConfig.getIgniteInterval(),
                IGNITE_MIN_FACTOR);
            color = ColorMath.scaleBrightness(color, brightness);
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
