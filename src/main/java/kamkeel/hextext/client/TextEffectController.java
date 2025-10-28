package kamkeel.hextext.client;

import kamkeel.hextext.util.ColorCodeUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * Tracks and applies HexText-specific animated text effects during rendering.
 */
public final class TextEffectController {

    private static final float RAINBOW_SPACING_DEGREES = 12.0f;
    private static final float RAINBOW_SPEED_DEGREES_PER_MS = 0.08f;
    private static final long IGNITE_PERIOD_MS = 200L;
    private static final float SHAKE_AMPLITUDE = 0.8f;
    private static final long SHAKE_PHASE_MS = 30L;

    private final Random random = new Random();

    private boolean rainbowActive;
    private boolean dinnerboneActive;
    private boolean igniteActive;
    private boolean shakeActive;

    private int rainbowStartGlyph;
    private long rainbowStartTime;
    private long igniteStartTime;
    private long shakeSeed;

    public void begin() {
        resetEffects();
        shakeSeed = Minecraft.getSystemTime();
    }

    public void resetEffects() {
        rainbowActive = false;
        dinnerboneActive = false;
        igniteActive = false;
        shakeActive = false;
        rainbowStartGlyph = 0;
        rainbowStartTime = 0L;
        igniteStartTime = 0L;
        shakeSeed = Minecraft.getSystemTime();
    }

    public void applyInstruction(RenderInstruction instruction, int glyphIndex) {
        switch (instruction.getType()) {
            case SET_RAINBOW:
                rainbowActive = instruction.isEnabled();
                if (rainbowActive) {
                    rainbowStartGlyph = glyphIndex;
                    rainbowStartTime = Minecraft.getSystemTime();
                }
                break;
            case SET_DINNERBONE:
                dinnerboneActive = instruction.isEnabled();
                break;
            case SET_IGNITE:
                igniteActive = instruction.isEnabled();
                if (igniteActive && igniteStartTime == 0L) {
                    igniteStartTime = Minecraft.getSystemTime();
                } else if (!igniteActive) {
                    igniteStartTime = 0L;
                }
                break;
            case SET_SHAKE:
                shakeActive = instruction.isEnabled();
                if (shakeActive) {
                    shakeSeed = Minecraft.getSystemTime();
                }
                break;
            case RESET_EFFECTS:
                resetEffects();
                break;
            default:
                break;
        }

        if (!rainbowActive) {
            rainbowStartTime = 0L;
        }
        if (!shakeActive) {
            shakeSeed = Minecraft.getSystemTime();
        }
        if (!igniteActive) {
            igniteStartTime = 0L;
        }
    }

    public GlyphEffects beforeGlyph(int glyphIndex, int baseColor) {
        if (!rainbowActive && !igniteActive && !dinnerboneActive && !shakeActive) {
            return null;
        }

        long now = Minecraft.getSystemTime();
        Integer colorOverride = null;

        if (rainbowActive) {
            float baseHue = (now - rainbowStartTime) * RAINBOW_SPEED_DEGREES_PER_MS;
            float hue = baseHue + (glyphIndex - rainbowStartGlyph) * RAINBOW_SPACING_DEGREES;
            colorOverride = ColorCodeUtils.hsvToRgb(hue, 1.0f, 1.0f);
        } else if (igniteActive) {
            if (igniteStartTime == 0L) {
                igniteStartTime = now;
            }
            long phase = (now - igniteStartTime) / IGNITE_PERIOD_MS;
            if ((phase & 1L) == 1L) {
                int dark = ColorCodeUtils.calculateShadowColor(baseColor);
                if (dark != baseColor) {
                    colorOverride = dark;
                }
            }
        }

        float offsetX = 0.0f;
        float offsetY = 0.0f;
        if (shakeActive) {
            long phase = now / SHAKE_PHASE_MS;
            random.setSeed(shakeSeed + glyphIndex * 341873128712L + phase * 132897987541L);
            offsetX = (random.nextFloat() - 0.5f) * 2.0f * SHAKE_AMPLITUDE;
            offsetY = (random.nextFloat() - 0.5f) * 2.0f * SHAKE_AMPLITUDE;
        }

        return new GlyphEffects(colorOverride, dinnerboneActive, offsetX, offsetY);
    }

    public static final class GlyphEffects {
        private final Integer colorOverride;
        private final boolean dinnerbone;
        private final float offsetX;
        private final float offsetY;
        private final boolean hasTransform;
        private boolean appliedTransform;

        GlyphEffects(Integer colorOverride, boolean dinnerbone, float offsetX, float offsetY) {
            this.colorOverride = colorOverride;
            this.dinnerbone = dinnerbone;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.hasTransform = dinnerbone || offsetX != 0.0f || offsetY != 0.0f;
        }

        public Integer getColorOverride() {
            return colorOverride;
        }

        public void begin(float posX, float posY, int fontHeight) {
            if (!hasTransform) {
                appliedTransform = false;
                return;
            }

            appliedTransform = true;
            GL11.glPushMatrix();
            GL11.glTranslatef(posX, posY, 0.0F);
            if (offsetX != 0.0f || offsetY != 0.0f) {
                GL11.glTranslatef(offsetX, offsetY, 0.0F);
            }
            if (dinnerbone) {
                GL11.glTranslatef(0.0F, fontHeight, 0.0F);
                GL11.glScalef(1.0F, -1.0F, 1.0F);
            }
            GL11.glTranslatef(-posX, -posY, 0.0F);
        }

        public void end() {
            if (appliedTransform) {
                GL11.glPopMatrix();
            }
        }
    }
}
