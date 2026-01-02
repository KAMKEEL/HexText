package kamkeel.hextext.network;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Dispatches configuration synchronisation packets to players as they join the server.
 */
public class ServerConfigSyncHandler {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) event.player;
        HexTextNetwork.channel.sendTo(SyncConfigMessage.fromServerConfig(), player);
    }
}
