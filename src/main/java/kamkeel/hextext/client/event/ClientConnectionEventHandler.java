package kamkeel.hextext.client.event;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.ClientProxy;
import kamkeel.hextext.client.config.ClientConfig;
import net.minecraft.client.Minecraft;

/**
 * Handles client connection lifecycle events so that remote feature toggles stay in sync with the
 * server the player is connected to.
 */
public class ClientConnectionEventHandler {

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        HexText.getActiveProxy().setRemoteServerHasHexText(false);
    }
}
