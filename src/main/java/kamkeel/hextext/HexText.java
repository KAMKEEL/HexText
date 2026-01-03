package kamkeel.hextext;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.relauncher.Side;

@Mod(
    modid = HexText.ID,
    name = HexText.NAME,
    version = HexText.VERSION,
    guiFactory = "kamkeel.hextext.client.config.HexTextGuiFactory"
)
public class HexText {

    public static final String NAME = "HexText";
    public static final String ID = "hextext";
    public static final String VERSION = "1.3";
    @SidedProxy(clientSide = "kamkeel.hextext.client.ClientProxy", serverSide = "kamkeel.hextext.CommonProxy")
    public static CommonProxy proxy;

    public static boolean rawRenderingEnabled = true;

    @Mod.Instance
    public static HexText instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent ev) {
        proxy.preInit(ev);

    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);

    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {

    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {

    }

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) {

    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);

    }

    public static Side side() {
        return FMLCommonHandler.instance().getEffectiveSide();
    }

    public static CommonProxy getActiveProxy() {
        return proxy;
    }
}
