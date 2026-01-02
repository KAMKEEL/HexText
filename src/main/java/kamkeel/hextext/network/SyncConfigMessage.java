package kamkeel.hextext.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import kamkeel.hextext.config.HexTextConfig;

/**
 * Synchronises server-controlled configuration toggles to clients when they join.
 */
public class SyncConfigMessage implements IMessage {

    public boolean universalAmpersand;
    public boolean chatAmpersands;
    public boolean signAmpersands;
    public boolean repairAmpersands;
    public boolean allowSignEditing;
    public boolean enableHtmlFormat;

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

    /**
     * Creates a message from the current server configuration.
     */
    public static SyncConfigMessage fromServerConfig() {
        return new SyncConfigMessage(
            HexTextConfig.isUniversalAmpersandEnabled(),
            HexTextConfig.isChatAmpersandConversionEnabled(),
            HexTextConfig.isSignAmpersandConversionEnabled(),
            HexTextConfig.isRepairAmpersandConversionEnabled(),
            HexTextConfig.isSignEditingAllowed(),
            HexTextConfig.isRgbHtmlFormatEnabled()
        );
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
}
