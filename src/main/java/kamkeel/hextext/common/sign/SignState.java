package kamkeel.hextext.common.sign;

import kamkeel.hextext.common.util.SignTextHelper;

/**
 * Behaviour contract injected into {@link net.minecraft.tileentity.TileEntitySign} via mixin.
 */
public interface SignState {

    SignSide hextext$getEditingSide();

    void hextext$setEditingSide(SignSide side);

    void hextext$prepareForEdit(SignSide side);

    void hextext$finishEdit();

    /**
     * Re-applies the currently active editing buffers so live text keeps rendering on the
     * correct face even if vanilla logic mutates {@code TileEntitySign#signText} mid-session.
     */
    void hextext$refreshEditingView();

    /**
     * Replaces all lines on the requested side, clamping them to the visible width and updating
     * any active editing buffer so UI state stays in sync.
     */
    void hextext$loadLines(SignSide side, String[] lines);

    boolean hextext$isWaxed();

    void hextext$setWaxed(boolean waxed);

    boolean hextext$isGlowing(SignSide side);

    boolean hextext$setGlowing(SignSide side, boolean glowing);

    boolean hextext$isOutlined(SignSide side);

    boolean hextext$setOutlined(SignSide side, boolean outlined);

    String[] hextext$getLines(SignSide side);

    default String hextext$getLine(SignSide side, int index) {
        String[] lines = hextext$getLines(side);
        return index >= 0 && index < lines.length ? lines[index] : "";
    }

    default void hextext$setLine(SignSide side, int index, String text) {
        String[] lines = hextext$getLines(side);
        if (index >= 0 && index < lines.length) {
            lines[index] = SignTextHelper.clampToVisibleLimit(text);
        }
    }
}
