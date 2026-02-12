package kamkeel.hextext.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import kamkeel.hextext.HexText;

/**
 * Centralises setup of HexText's SimpleNetworkWrapper channel and message registrations.
 */
public final class HexTextNetwork {

    public static SimpleNetworkWrapper channel;

    private HexTextNetwork() {
    }

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(HexText.ID);
        channel.registerMessage(SyncConfigHandler.class, SyncConfigMessage.class, 0, Side.CLIENT);
        channel.registerMessage(SignEditRequestHandler.class, SignEditRequestMessage.class, 1, Side.SERVER);
    }
}
