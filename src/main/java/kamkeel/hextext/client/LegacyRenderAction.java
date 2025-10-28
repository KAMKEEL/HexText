package kamkeel.hextext.client;

/**
 * Represents a deferred colour-stack or formatting action for the legacy font mixin.
 */
public final class LegacyRenderAction {

    public enum Type {
        APPLY_RGB,
        PUSH_RGB,
        POP_COLOR
    }

    private final Type type;
    private final int rgb;
    private final boolean clearStack;

    private LegacyRenderAction(Type type, int rgb, boolean clearStack) {
        this.type = type;
        this.rgb = rgb;
        this.clearStack = clearStack;
    }

    public static LegacyRenderAction apply(int rgb, boolean clearStack) {
        return new LegacyRenderAction(Type.APPLY_RGB, rgb, clearStack);
    }

    public static LegacyRenderAction push(int rgb) {
        return new LegacyRenderAction(Type.PUSH_RGB, rgb, false);
    }

    public static LegacyRenderAction pop() {
        return new LegacyRenderAction(Type.POP_COLOR, 0, false);
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
