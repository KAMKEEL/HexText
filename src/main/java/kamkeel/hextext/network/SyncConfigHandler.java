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
        // Schedule on main thread to ensure proper ordering with connection events.
        // The connection event schedules first (it fires before packets can arrive),
        // so this will always run after the connection handler has reset configs.
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {
                if (HexText.getActiveProxy() instanceof ClientProxy) {
                    ((ClientProxy) HexText.getActiveProxy()).applyServerConfig(
                        message.universalAmpersand,
                        message.chatAmpersands,
                        message.signAmpersands,
                        message.repairAmpersands,
                        message.allowSignEditing,
                        message.enableHtmlFormat
                    );
                }
            }
        });
        return null;
    }
}
