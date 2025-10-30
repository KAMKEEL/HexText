package kamkeel.hextext.common.sign;

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

    public void setEditSide(SignSide side);

    public SignSide getEditSide();
}
