package kamkeel.hextext;


import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommonProxy {
    public static final Logger LOGGER = LogManager.getLogger(HexText.ID);

    public static void eventsInit() {


    }

    public void preInit(FMLPreInitializationEvent ev) {
        eventsInit();
    }

    public void init(FMLInitializationEvent ev) {
    }

    public void postInit(FMLPostInitializationEvent ev) {
    }
}
