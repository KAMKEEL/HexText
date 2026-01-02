package kamkeel.hextext.client.event;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import kamkeel.hextext.client.ClientProxy;
import kamkeel.hextext.client.config.ClientConfig;
import net.minecraft.client.Minecraft;

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
        // Schedule on main thread to avoid race conditions with packet handlers.
        // All configs start disabled until the server sends its sync packet.
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {
                clientConfig.apply(false, false, false, false, false, false);
                clientProxy.setRemoteServerHasHexText(false);
            }
        });
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // Schedule on main thread for consistency.
        Minecraft.getMinecraft().func_152344_a(new Runnable() {
            @Override
            public void run() {
                clientConfig.resetToLocalConfig();
                clientProxy.setRemoteServerHasHexText(true);
            }
        });
    }
}
