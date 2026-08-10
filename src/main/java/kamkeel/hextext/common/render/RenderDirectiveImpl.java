package kamkeel.hextext.common.render;

import kamkeel.hextext.api.rendering.RenderDirective;

/**
 * Represents a deferred colour-stack or formatting action emitted by the render pre-processor.
 */
public final class RenderDirectiveImpl implements RenderDirective {

    public enum Type implements InstructionType {
        APPLY_RGB,
        PUSH_RGB,
        POP_COLOR,
        RESET_TO_BASE,
        APPLY_VANILLA_COLOR,
        SET_RANDOM,
        SET_BOLD,
        SET_STRIKETHROUGH,
        SET_UNDERLINE,
        SET_ITALIC,
        SET_RAINBOW,
        SET_DINNERBONE,
        SET_IGNITE,
        SET_SHAKE,
        SET_WAVE,
        SET_GRADIENT,
        SET_SHADOW_COLOR
    }

    private final Type type;
    private final int rgb;
    /**
     * The far end of a gradient, zero elsewhere. {@code rgb} starts the ramp, this ends
     * it, and {@code parameter} is how many glyphs it has to get there in.
     */
    private final int secondaryRgb;
    private final boolean clearStack;
    private final int parameter;
    private final boolean enabled;
    private final boolean resetFormatting;

    private RenderDirectiveImpl(Type type, int rgb, boolean clearStack, int parameter, boolean enabled,
                                boolean resetFormatting) {
        this(type, rgb, 0, clearStack, parameter, enabled, resetFormatting);
    }

    private RenderDirectiveImpl(Type type, int rgb, int secondaryRgb, boolean clearStack, int parameter,
                                boolean enabled, boolean resetFormatting) {
        this.secondaryRgb = secondaryRgb;
        this.type = type;
        this.rgb = rgb;
        this.clearStack = clearStack;
        this.parameter = parameter;
        this.enabled = enabled;
        this.resetFormatting = resetFormatting;
    }

    public static RenderDirective apply(int rgb, boolean clearStack) {
        return new RenderDirectiveImpl(Type.APPLY_RGB, rgb, clearStack, 0, false, true);
    }

    public static RenderDirective push(int rgb) {
        return new RenderDirectiveImpl(Type.PUSH_RGB, rgb, false, 0, false, true);
    }

    public static RenderDirective pop() {
        return new RenderDirectiveImpl(Type.POP_COLOR, 0, false, 0, false, false);
    }

    public static RenderDirective resetToBase() {
        return new RenderDirectiveImpl(Type.RESET_TO_BASE, 0, true, 0, false, true);
    }

    public static RenderDirective applyVanillaColor(int colorIndex) {
        return new RenderDirectiveImpl(Type.APPLY_VANILLA_COLOR, 0, true, colorIndex, false, true);
    }

    public static RenderDirective setRandom(boolean enabled) {
        return new RenderDirectiveImpl(Type.SET_RANDOM, 0, false, 0, enabled, false);
    }

    public static RenderDirective setBold(boolean enabled) {
        return new RenderDirectiveImpl(Type.SET_BOLD, 0, false, 0, enabled, false);
    }

    public static RenderDirective setStrikethrough(boolean enabled) {
        return new RenderDirectiveImpl(Type.SET_STRIKETHROUGH, 0, false, 0, enabled, false);
    }

    public static RenderDirective setUnderline(boolean enabled) {
        return new RenderDirectiveImpl(Type.SET_UNDERLINE, 0, false, 0, enabled, false);
    }

    public static RenderDirective setItalic(boolean enabled) {
        return new RenderDirectiveImpl(Type.SET_ITALIC, 0, false, 0, enabled, false);
    }

    public static RenderDirective setRainbow(boolean enabled, int anchorIndex) {
        return new RenderDirectiveImpl(Type.SET_RAINBOW, 0, true, anchorIndex, enabled, true);
    }

    public static RenderDirective setDinnerbone(boolean enabled) {
        return new RenderDirectiveImpl(Type.SET_DINNERBONE, 0, false, 0, enabled, false);
    }

    public static RenderDirective setIgnite(boolean enabled) {
        return new RenderDirectiveImpl(Type.SET_IGNITE, 0, false, 0, enabled, false);
    }

    /**
     * Tints the shadow the text already casts. Only the colour of a shadow being drawn
     * anyway - the caller decides for the whole string whether there is a shadow pass.
     *
     * @param enabled false restores the darkened base colour
     */
    public static RenderDirective setShadowColor(int rgb, boolean enabled) {
        return new RenderDirectiveImpl(Type.SET_SHADOW_COLOR, rgb, false, 0, enabled, false);
    }

    public int getSecondaryRgb() {
        return secondaryRgb;
    }

    /**
     * A gradient across the glyphs that follow. Clears the colour stack like an inline
     * hex, but keeps styles: the width walkers carry them through a gradient token, and
     * dropping them measured one string and drew another.
     *
     * @param span how many visible glyphs it is spread over
     */
    public static RenderDirective setGradient(int startRgb, int endRgb, int span) {
        return new RenderDirectiveImpl(Type.SET_GRADIENT, startRgb, endRgb, true, span, true, false);
    }

    /** Wave is positional like shake, but a travelling sine rather than noise. */
    public static RenderDirective setWave(boolean enabled) {
        return new RenderDirectiveImpl(Type.SET_WAVE, 0, false, 0, enabled, false);
    }

    public static RenderDirective setShake(boolean enabled) {
        return new RenderDirectiveImpl(Type.SET_SHAKE, 0, false, 0, enabled, false);
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public int getRgb() {
        return rgb;
    }

    @Override
    public boolean shouldClearStack() {
        return clearStack;
    }

    @Override
    public int getParameter() {
        return parameter;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean resetsFormatting() {
        return resetFormatting;
    }
}
