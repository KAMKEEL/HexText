package kamkeel.hextext.client;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.client.config.ClientConfig;

public class ClientProxy extends CommonProxy {

    private final ClientConfig clientConfig = new ClientConfig();

    public static void eventsInit() {

    }

    @Override
    public void preInit(FMLPreInitializationEvent ev) {
        super.preInit(ev);

    }

    @Override
    public void init(FMLInitializationEvent ev) {
        super.init(ev);
        eventsInit();
    }

    @Override
    public void postInit(FMLPostInitializationEvent ev) {
        super.postInit(ev);
    }

    @Override
    public boolean allowUniversalAmpersand() {
        return clientConfig.allowUniversalAmpersand();
    }

    @Override
    public boolean convertAmpersandsInChat() {
        return allowUniversalAmpersand() || clientConfig.convertAmpersandsInChat();
    }

    @Override
    public boolean convertAmpersandsOnSigns() {
        return allowUniversalAmpersand() || clientConfig.convertAmpersandsOnSigns();
    }

    @Override
    public boolean convertAmpersandsInRepairs() {
        return allowUniversalAmpersand() || clientConfig.convertAmpersandsInRepairs();
    }

    @Override
    public boolean allowSignEditing() {
        return clientConfig.allowSignEditing();
    }

    @Override
    public boolean allowHtmlFormatting() {
        return clientConfig.enableHtmlFormat();
    }

    @Override
    public void applyServerConfig(boolean universalAmpersand, boolean chatAmpersands, boolean signAmpersands,
        boolean repairAmpersands, boolean allowSignEditing, boolean enableHtmlFormat) {
        clientConfig.apply(universalAmpersand, chatAmpersands, signAmpersands, repairAmpersands, allowSignEditing,
            enableHtmlFormat);
    }
}
