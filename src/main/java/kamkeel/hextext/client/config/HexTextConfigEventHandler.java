package kamkeel.hextext.client.config;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import kamkeel.hextext.HexText;
import kamkeel.hextext.config.HexTextConfig;

/**
 * Listens for configuration changes and resynchronizes HexText settings.
 */
public class HexTextConfigEventHandler {

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (HexText.ID.equals(event.modID)) {
            HexTextConfig.sync();
        }
    }
}
