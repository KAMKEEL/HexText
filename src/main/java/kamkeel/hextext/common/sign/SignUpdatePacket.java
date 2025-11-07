package kamkeel.hextext.common.sign;

import kamkeel.hextext.api.sign.SignSide;

public interface SignUpdatePacket {

    void setSide(SignSide side);

    SignSide getSide();
}
