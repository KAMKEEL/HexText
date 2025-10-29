package kamkeel.hextext;


import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import kamkeel.hextext.config.HexTextConfig;
import kamkeel.hextext.config.HexTextConfigEventHandler;

public class CommonProxy {
    public static final Logger LOGGER = LogManager.getLogger(HexText.ID);
    private static boolean eventsRegistered;

    public static void eventsInit() {
        if (!eventsRegistered) {
            FMLCommonHandler.instance().bus().register(new HexTextConfigEventHandler());
            eventsRegistered = true;
        }
    }

    public void preInit(FMLPreInitializationEvent ev) {
        HexTextConfig.init(ev.getSuggestedConfigurationFile());
        eventsInit();
    }

    public void init(FMLInitializationEvent ev) {
    }

    public void postInit(FMLPostInitializationEvent ev) {
    }
}
