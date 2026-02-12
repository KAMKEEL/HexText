package kamkeel.hextext.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import kamkeel.hextext.HexText;
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
    public String[] bannedSignPatterns = new String[0];

    public SyncConfigMessage() {
    }

    public SyncConfigMessage(boolean universalAmpersand, boolean chatAmpersands, boolean signAmpersands,
                             boolean repairAmpersands, boolean allowSignEditing, boolean enableHtmlFormat,
                             String[] bannedSignPatterns) {
        this.universalAmpersand = universalAmpersand;
        this.chatAmpersands = chatAmpersands;
        this.signAmpersands = signAmpersands;
        this.repairAmpersands = repairAmpersands;
        this.allowSignEditing = allowSignEditing;
        this.enableHtmlFormat = enableHtmlFormat;
        this.bannedSignPatterns = bannedSignPatterns != null ? bannedSignPatterns : new String[0];
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
            HexTextConfig.isRgbHtmlFormatEnabled(),
            HexTextConfig.getBannedSignPatterns()
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

        if (buf.isReadable()) {
            int count = buf.readInt();
            bannedSignPatterns = new String[count];
            for (int i = 0; i < count; i++) {
                bannedSignPatterns[i] = ByteBufUtils.readUTF8String(buf);
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(universalAmpersand);
        buf.writeBoolean(chatAmpersands);
        buf.writeBoolean(signAmpersands);
        buf.writeBoolean(repairAmpersands);
        buf.writeBoolean(allowSignEditing);
        buf.writeBoolean(enableHtmlFormat);

        buf.writeInt(bannedSignPatterns.length);
        for (String pattern : bannedSignPatterns) {
            ByteBufUtils.writeUTF8String(buf, pattern);
        }
    }
}
