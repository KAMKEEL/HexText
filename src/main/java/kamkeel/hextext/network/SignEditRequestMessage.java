package kamkeel.hextext.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import kamkeel.hextext.api.sign.SignSide;

/**
 * Sent by the client to request opening a sign for editing.
 */
public class SignEditRequestMessage implements IMessage {

    public int x;
    public int y;
    public int z;
    public int side;

    public SignEditRequestMessage() {
    }

    public SignEditRequestMessage(int x, int y, int z, SignSide side) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.side = side.ordinal();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        side = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeByte(side);
    }

    public SignSide getSignSide() {
        SignSide[] values = SignSide.values();
        if (side >= 0 && side < values.length) {
            return values[side];
        }
        return SignSide.FRONT;
    }
}
