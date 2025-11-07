package kamkeel.hextext.api.sign;

import net.minecraft.tileentity.TileEntitySign;

/**
 * High-level helpers for interacting with HexText-enhanced sign tile entities.
 */
public interface SignStateApi {

    /**
     * Checks whether the supplied tile entity has been converted to a HexText sign.
     */
    boolean isHexTextSign(TileEntitySign sign);

    /**
     * Returns {@code true} when the sign has been waxed to prevent edits.
     */
    boolean isWaxed(TileEntitySign sign);

    /**
     * Updates the waxed state.
     */
    void setWaxed(TileEntitySign sign, boolean waxed);

    /**
     * Returns whether the supplied side glows.
     */
    boolean isGlowing(TileEntitySign sign, SignSide side);

    /**
     * Updates the glowing state for the supplied side.
     */
    boolean setGlowing(TileEntitySign sign, SignSide side, boolean glowing);

    /**
     * Returns whether the supplied side renders with an outline.
     */
    boolean isOutlined(TileEntitySign sign, SignSide side);

    /**
     * Updates the outline state for the supplied side.
     */
    boolean setOutlined(TileEntitySign sign, SignSide side, boolean outlined);

    /**
     * Fetches the text for the supplied side.
     */
    String[] getLines(TileEntitySign sign, SignSide side);

    /**
     * Sets the editing side.
     */
    void setEditingSide(TileEntitySign sign, SignSide side);

    /**
     * Returns the active editing side.
     */
    SignSide getEditingSide(TileEntitySign sign);

    /**
     * Determines the side of the sign that the player interacted with.
     */
    SignSide determineSide(TileEntitySign sign, double playerX, double playerZ, float hitX, float hitZ);
}
