package kamkeel.hextext;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import kamkeel.hextext.api.HexTextApi;
import kamkeel.hextext.client.config.HexTextConfigEventHandler;
import kamkeel.hextext.common.api.HexTextApiBootstrap;
import kamkeel.hextext.config.HexTextConfig;
import kamkeel.hextext.network.HexTextNetwork;
import kamkeel.hextext.network.ServerConfigSyncHandler;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommonProxy {
    public static final Logger LOGGER = LogManager.getLogger(HexText.ID);
    private static boolean eventsRegistered;

    private static final HexTextConfigEventHandler CONFIG_EVENT_HANDLER = new HexTextConfigEventHandler();
    private static final ServerConfigSyncHandler SERVER_CONFIG_SYNC_HANDLER = new ServerConfigSyncHandler();

    public static void eventsInit() {
        if (!eventsRegistered) {
            FMLCommonHandler.instance().bus().register(CONFIG_EVENT_HANDLER);
            MinecraftForge.EVENT_BUS.register(SERVER_CONFIG_SYNC_HANDLER);
            eventsRegistered = true;
        }
    }

    public void preInit(FMLPreInitializationEvent ev) {
        HexTextApiBootstrap.initialize();
        HexTextConfig.init(ev.getSuggestedConfigurationFile());
        HexTextNetwork.init();
        eventsInit();
    }

    public void init(FMLInitializationEvent ev) {
        HexTextApi.signInteractions().registerDefaults();
    }

    public void postInit(FMLPostInitializationEvent ev) {
    }

    public boolean allowUniversalAmpersand() {
        return HexTextConfig.isUniversalAmpersandEnabled();
    }

    public boolean convertAmpersandsInChat() {
        return allowUniversalAmpersand() || HexTextConfig.isChatAmpersandConversionEnabled();
    }

    public boolean convertAmpersandsOnSigns() {
        return allowUniversalAmpersand() || HexTextConfig.isSignAmpersandConversionEnabled();
    }

    public boolean convertAmpersandsInRepairs() {
        return allowUniversalAmpersand() || HexTextConfig.isRepairAmpersandConversionEnabled();
    }

    public boolean allowSignEditing() {
        return HexTextConfig.isSignEditingAllowed();
    }

    public boolean allowHtmlFormatting() {
        return HexTextConfig.isRgbHtmlFormatEnabled();
    }

    public void applyServerConfig(boolean universalAmpersand, boolean chatAmpersands, boolean signAmpersands,
        boolean repairAmpersands, boolean allowSignEditing, boolean enableHtmlFormat) {
        // Server already owns the authoritative configuration, so nothing further needs to happen here.
    }
}
