package kamkeel.hextext.client;

/**
 * Represents a deferred colour-stack or formatting action emitted by the render pre-processor.
 */
public final class RenderInstruction {

    public enum Type {
        APPLY_RGB,
        PUSH_RGB,
        POP_COLOR,
        RESET_TO_BASE
    }

    private final Type type;
    private final int rgb;
    private final boolean clearStack;

    private RenderInstruction(Type type, int rgb, boolean clearStack) {
        this.type = type;
        this.rgb = rgb;
        this.clearStack = clearStack;
    }

    public static RenderInstruction apply(int rgb, boolean clearStack) {
        return new RenderInstruction(Type.APPLY_RGB, rgb, clearStack);
    }

    public static RenderInstruction push(int rgb) {
        return new RenderInstruction(Type.PUSH_RGB, rgb, false);
    }

    public static RenderInstruction pop() {
        return new RenderInstruction(Type.POP_COLOR, 0, false);
    }

    public static RenderInstruction resetToBase() {
        return new RenderInstruction(Type.RESET_TO_BASE, 0, true);
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
}
