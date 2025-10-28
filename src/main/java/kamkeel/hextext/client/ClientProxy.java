package kamkeel.hextext.client;


import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import kamkeel.hextext.CommonProxy;

public class ClientProxy extends CommonProxy {

    public static void eventsInit() {

    }

    @Override
    public void preInit(FMLPreInitializationEvent ev) {
        super.preInit(ev);

    }

    public void init(FMLInitializationEvent ev) {
        super.init(ev);
        eventsInit();
    }

    public void postInit(FMLPostInitializationEvent ev) {
        super.postInit(ev);
    }
}
