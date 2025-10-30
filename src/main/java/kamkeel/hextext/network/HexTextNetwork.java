package kamkeel.hextext.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import kamkeel.hextext.HexText;

/**
 * Centralises setup of HexText's SimpleNetworkWrapper channel and message registrations.
 */
public final class HexTextNetwork {

    private static final SimpleNetworkWrapper CHANNEL =
        NetworkRegistry.INSTANCE.newSimpleChannel(HexText.ID);

    private static boolean initialised;

    private HexTextNetwork() {
    }

    public static void init() {
        if (initialised) {
            return;
        }

        int discriminator = 0;
        CHANNEL.registerMessage(SyncConfigMessage.Handler.class, SyncConfigMessage.class, discriminator++, Side.CLIENT);

        initialised = true;
    }

    public static SimpleNetworkWrapper channel() {
        return CHANNEL;
    }
}
