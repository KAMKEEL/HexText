package kamkeel.hextext.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.ClientProxy;

/**
 * Synchronises server-controlled configuration toggles to clients when they join.
 */
public class SyncConfigMessage implements IMessage {

    private boolean allowAmpersand;
    private boolean allowSignEditing;
    private boolean enableHtmlFormat;

    public SyncConfigMessage() {
    }

    public SyncConfigMessage(boolean allowAmpersand, boolean allowSignEditing, boolean enableHtmlFormat) {
        this.allowAmpersand = allowAmpersand;
        this.allowSignEditing = allowSignEditing;
        this.enableHtmlFormat = enableHtmlFormat;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        allowAmpersand = buf.readBoolean();
        allowSignEditing = buf.readBoolean();
        enableHtmlFormat = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(allowAmpersand);
        buf.writeBoolean(allowSignEditing);
        buf.writeBoolean(enableHtmlFormat);
    }

    public boolean allowAmpersand() {
        return allowAmpersand;
    }

    public boolean allowSignEditing() {
        return allowSignEditing;
    }

    public boolean enableHtmlFormat() {
        return enableHtmlFormat;
    }

    public static class Handler implements IMessageHandler<SyncConfigMessage, IMessage> {

        @Override
        public IMessage onMessage(final SyncConfigMessage message, final MessageContext ctx) {
            if (ctx.side != Side.CLIENT) {
                return null;
            }

            if (HexText.getActiveProxy() instanceof ClientProxy) {
                ((ClientProxy) HexText.getActiveProxy()).applyServerConfig(message.allowAmpersand(),
                    message.allowSignEditing(), message.enableHtmlFormat());
            }
            return null;
        }
    }
}
