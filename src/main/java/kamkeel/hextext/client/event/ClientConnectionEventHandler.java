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
        // Always start with all configs disabled and wait for the server to sync its settings.
        // Even single-player worlds run an integrated server that will send the sync message.
        clientConfig.apply(false, false, false, false, false, false);
        clientProxy.setRemoteServerHasHexText(false);
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        clientConfig.resetToLocalConfig();
        clientProxy.setRemoteServerHasHexText(true);
    }
}
