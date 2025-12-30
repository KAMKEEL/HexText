package kamkeel.hextext.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.ClientProxy;
import net.minecraft.client.Minecraft;

/**
 * Synchronises server-controlled configuration toggles to clients when they join.
 */
public class SyncConfigMessage implements IMessage {

    private boolean universalAmpersand;
    private boolean chatAmpersands;
    private boolean signAmpersands;
    private boolean repairAmpersands;
    private boolean allowSignEditing;
    private boolean enableHtmlFormat;

    public SyncConfigMessage() {
    }

    public SyncConfigMessage(boolean universalAmpersand, boolean chatAmpersands, boolean signAmpersands,
        boolean repairAmpersands, boolean allowSignEditing, boolean enableHtmlFormat) {
        this.universalAmpersand = universalAmpersand;
        this.chatAmpersands = chatAmpersands;
        this.signAmpersands = signAmpersands;
        this.repairAmpersands = repairAmpersands;
        this.allowSignEditing = allowSignEditing;
        this.enableHtmlFormat = enableHtmlFormat;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        universalAmpersand = buf.readBoolean();
        chatAmpersands = buf.readBoolean();
        signAmpersands = buf.readBoolean();
        repairAmpersands = buf.readBoolean();
        allowSignEditing = buf.readBoolean();
        enableHtmlFormat = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(universalAmpersand);
        buf.writeBoolean(chatAmpersands);
        buf.writeBoolean(signAmpersands);
        buf.writeBoolean(repairAmpersands);
        buf.writeBoolean(allowSignEditing);
        buf.writeBoolean(enableHtmlFormat);
    }

    public boolean allowUniversalAmpersand() {
        return universalAmpersand;
    }

    public boolean convertAmpersandsInChat() {
        return chatAmpersands;
    }

    public boolean convertAmpersandsOnSigns() {
        return signAmpersands;
    }

    public boolean convertAmpersandsInRepairs() {
        return repairAmpersands;
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

            // Schedule config application on the main thread to ensure it runs after
            // connection events have been processed, avoiding race conditions.
            Minecraft.getMinecraft().func_152344_a(new Runnable() {
                @Override
                public void run() {
                    if (HexText.getActiveProxy() instanceof ClientProxy) {
                        ((ClientProxy) HexText.getActiveProxy()).applyServerConfig(
                            message.allowUniversalAmpersand(),
                            message.convertAmpersandsInChat(),
                            message.convertAmpersandsOnSigns(),
                            message.convertAmpersandsInRepairs(),
                            message.allowSignEditing(),
                            message.enableHtmlFormat());
                    }
                }
            });
            return null;
        }
    }
}
