package kamkeel.hextext.client;

/**
 * Represents a deferred colour-stack or formatting action emitted by the render pre-processor.
 */
public final class RenderInstruction {

    public enum Type {
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
        SET_SHAKE
    }

    private final Type type;
    private final int rgb;
    private final boolean clearStack;
    private final int parameter;
    private final boolean enabled;
    private final boolean resetFormatting;

    private RenderInstruction(Type type, int rgb, boolean clearStack, int parameter, boolean enabled,
            boolean resetFormatting) {
        this.type = type;
        this.rgb = rgb;
        this.clearStack = clearStack;
        this.parameter = parameter;
        this.enabled = enabled;
        this.resetFormatting = resetFormatting;
    }

    public static RenderInstruction apply(int rgb, boolean clearStack) {
        return new RenderInstruction(Type.APPLY_RGB, rgb, clearStack, 0, false, true);
    }

    public static RenderInstruction push(int rgb) {
        return new RenderInstruction(Type.PUSH_RGB, rgb, false, 0, false, true);
    }

    public static RenderInstruction pop() {
        return new RenderInstruction(Type.POP_COLOR, 0, false, 0, false, false);
    }

    public static RenderInstruction resetToBase() {
        return new RenderInstruction(Type.RESET_TO_BASE, 0, true, 0, false, true);
    }

    public static RenderInstruction applyVanillaColor(int colorIndex) {
        return new RenderInstruction(Type.APPLY_VANILLA_COLOR, 0, true, colorIndex, false, true);
    }

    public static RenderInstruction setRandom(boolean enabled) {
        return new RenderInstruction(Type.SET_RANDOM, 0, false, 0, enabled, false);
    }

    public static RenderInstruction setBold(boolean enabled) {
        return new RenderInstruction(Type.SET_BOLD, 0, false, 0, enabled, false);
    }

    public static RenderInstruction setStrikethrough(boolean enabled) {
        return new RenderInstruction(Type.SET_STRIKETHROUGH, 0, false, 0, enabled, false);
    }

    public static RenderInstruction setUnderline(boolean enabled) {
        return new RenderInstruction(Type.SET_UNDERLINE, 0, false, 0, enabled, false);
    }

    public static RenderInstruction setItalic(boolean enabled) {
        return new RenderInstruction(Type.SET_ITALIC, 0, false, 0, enabled, false);
    }

    public static RenderInstruction setRainbow(boolean enabled, int anchorIndex) {
        return new RenderInstruction(Type.SET_RAINBOW, 0, true, anchorIndex, enabled, false);
    }

    public static RenderInstruction setDinnerbone(boolean enabled) {
        return new RenderInstruction(Type.SET_DINNERBONE, 0, false, 0, enabled, false);
    }

    public static RenderInstruction setIgnite(boolean enabled) {
        return new RenderInstruction(Type.SET_IGNITE, 0, false, 0, enabled, false);
    }

    public static RenderInstruction setShake(boolean enabled) {
        return new RenderInstruction(Type.SET_SHAKE, 0, false, 0, enabled, false);
    }

    public Type getType() {
        return type;
    }

    public int getRgb() {
        return rgb;
    }

    public boolean shouldClearStack() {
        return clearStack;
    }

    public int getParameter() {
        return parameter;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean resetsFormatting() {
        return resetFormatting;
    }
}
