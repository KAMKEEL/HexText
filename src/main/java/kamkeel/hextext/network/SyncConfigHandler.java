package kamkeel.hextext.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.ClientProxy;
import net.minecraft.client.Minecraft;

/**
 * Handles incoming config sync messages on the client side.
 */
public class SyncConfigHandler implements IMessageHandler<SyncConfigMessage, IMessage> {

    @SideOnly(Side.CLIENT)
    @Override
    public IMessage onMessage(final SyncConfigMessage message, MessageContext ctx) {
        HexText.getActiveProxy().applyServerConfig(
            message.universalAmpersand,
            message.chatAmpersands,
            message.signAmpersands,
            message.repairAmpersands,
            message.allowSignEditing,
            message.enableHtmlFormat,
            message.bannedSignPatterns
        );
        HexText.getActiveProxy().setRemoteServerHasHexText(true);
        return null;
    }
}
