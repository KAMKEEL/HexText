package kamkeel.hextext.client.event;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import kamkeel.hextext.client.ClientProxy;
import kamkeel.hextext.client.config.ClientConfig;

/**
 * Handles client connection lifecycle events so that remote feature toggles stay in sync with the
 * server the player is connected to.
 */
public class ClientConnectionEventHandler {

    private final ClientProxy clientProxy;
    private final ClientConfig clientConfig;

    public ClientConnectionEventHandler(ClientProxy clientProxy, ClientConfig clientConfig) {
        this.clientProxy = clientProxy;
        this.clientConfig = clientConfig;
    }

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        // Mark that we haven't received a HexText sync packet yet.
        // The server will send a sync packet if it has HexText installed.
        clientProxy.setRemoteServerHasHexText(false);
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // Reset current values back to the original local config values
        clientConfig.resetToOriginal();
        clientProxy.setRemoteServerHasHexText(true);
    }
}
