package kamkeel.hextext.client;

import kamkeel.hextext.util.ColorCodeUtils;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks colour stack state for the font renderer so mixin logic can stay lean.
 */
public final class ColorStateTracker {

    private final Deque<Integer> colorStack = new ArrayDeque<>();
    private final Deque<Integer> effectBaseStack = new ArrayDeque<>();

    private int baseColor;
    private int currentColor;
    private boolean shadowPass;

    public void begin(int baseColor, boolean shadowPass) {
        this.baseColor = baseColor & 0xFFFFFF;
        this.currentColor = this.baseColor;
        this.shadowPass = shadowPass;
        colorStack.clear();
        effectBaseStack.clear();
    }

    public int getBaseColor() {
        return baseColor;
    }

    public int getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(int color) {
        this.currentColor = color & 0xFFFFFF;
    }

    public int applyRgb(int rgb, boolean clearStacks, TextEffectController effects, boolean renderingShadow) {
        if (clearStacks) {
            wipeStacks();
        }
        int applied = convert(rgb);
        currentColor = applied;
        resetEffects(effects, renderingShadow, applied);
        return applied;
    }

    public int applyVanillaColor(int colorIndex, int[] palette, boolean clearStacks, TextEffectController effects,
            boolean renderingShadow) {
        if (clearStacks) {
            wipeStacks();
        }
        int applied = baseColor;
        int clampedIndex = Math.max(0, Math.min(colorIndex, 15));
        if (palette != null) {
            int paletteIndex = shadowPass ? clampedIndex + 16 : clampedIndex;
            if (paletteIndex >= 0 && paletteIndex < palette.length) {
                applied = palette[paletteIndex] & 0xFFFFFF;
            }
        }
        currentColor = applied;
        resetEffects(effects, renderingShadow, applied);
        return applied;
    }

    public int push(int rgb, TextEffectController effects, boolean renderingShadow) {
        colorStack.push(currentColor);
        if (effects != null) {
            effectBaseStack.push(effects.getBaseColor());
            effects.resetDynamicEffects();
        }
        int applied = convert(rgb);
        currentColor = applied;
        if (effects != null && !renderingShadow) {
            effects.updateBaseColor(applied);
        }
        return applied;
    }

    public int pop(TextEffectController effects, boolean renderingShadow) {
        currentColor = colorStack.isEmpty() ? baseColor : colorStack.pop();
        if (effects != null) {
            effects.resetDynamicEffects();
            int base = effectBaseStack.isEmpty() ? baseColor : effectBaseStack.pop();
            if (!renderingShadow) {
                effects.updateBaseColor(base);
            }
        }
        return currentColor;
    }

    public int resetToBase(TextEffectController effects, boolean renderingShadow) {
        wipeStacks();
        currentColor = baseColor;
        resetEffects(effects, renderingShadow, baseColor);
        return currentColor;
    }

    public void clearStacks() {
        wipeStacks();
    }

    private int convert(int rgb) {
        int color = rgb & 0xFFFFFF;
        return shadowPass ? ColorCodeUtils.calculateShadowColor(color) : color;
    }

    private void wipeStacks() {
        colorStack.clear();
        effectBaseStack.clear();
    }

    private void resetEffects(TextEffectController effects, boolean renderingShadow, int applied) {
        if (effects == null) {
            return;
        }
        effects.resetDynamicEffects();
        if (!renderingShadow) {
            effects.updateBaseColor(applied);
        }
    }
}
