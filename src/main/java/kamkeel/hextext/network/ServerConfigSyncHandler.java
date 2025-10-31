package kamkeel.hextext.network;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import kamkeel.hextext.config.HexTextConfig;
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
        SyncConfigMessage message = new SyncConfigMessage(
            HexTextConfig.isUniversalAmpersandEnabled(),
            HexTextConfig.isChatAmpersandConversionEnabled(),
            HexTextConfig.isSignAmpersandConversionEnabled(),
            HexTextConfig.isRepairAmpersandConversionEnabled(),
            HexTextConfig.isSignEditingAllowed(),
            HexTextConfig.isRgbHtmlFormatEnabled()
        );
        HexTextNetwork.channel().sendTo(message, player);
    }
}
