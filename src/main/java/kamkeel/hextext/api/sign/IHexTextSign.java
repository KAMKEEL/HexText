package kamkeel.hextext.api.sign;

/**
 * Behaviour contract injected into {@link net.minecraft.tileentity.TileEntitySign} via mixin.
 */
public interface IHexTextSign {

    boolean isWaxed();

    void setWaxed(boolean waxed);

    boolean isGlowing(SignSide side);

    boolean setGlowing(SignSide side, boolean glowing);

    boolean isOutlined(SignSide side);

    boolean setOutlined(SignSide side, boolean outlined);

    String[] getLines(SignSide side);

    void setEditSide(SignSide side);

    SignSide getEditSide();
}
