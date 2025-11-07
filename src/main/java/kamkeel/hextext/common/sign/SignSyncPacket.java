package kamkeel.hextext.common.sign;

import kamkeel.hextext.api.sign.SignSide;

public interface SignSyncPacket {

    void setBackText(String[] lines);

    String[] getBackText();

    void setGlowing(SignSide side, boolean glowing);

    boolean isGlowing(SignSide side);

    void setOutlined(SignSide side, boolean outlined);

    boolean isOutlined(SignSide side);

    void setWaxed(boolean waxed);

    boolean isWaxed();
}
