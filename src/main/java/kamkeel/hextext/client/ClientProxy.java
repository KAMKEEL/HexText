package kamkeel.hextext.client;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.client.compat.AngelicaClientCompat;
import kamkeel.hextext.client.compat.GTNHLibTextCompat;
import kamkeel.hextext.client.config.ClientConfig;
import kamkeel.hextext.client.event.ClientConnectionEventHandler;

public class ClientProxy extends CommonProxy {

    private final ClientConfig clientConfig = new ClientConfig();
    private final ClientConnectionEventHandler connectionEventHandler = new ClientConnectionEventHandler();
    private boolean clientEventsRegistered;
    private boolean remoteServerHasHexText = false;

    @Override
    public void preInit(FMLPreInitializationEvent ev) {
        super.preInit(ev);
    }

    @Override
    public void init(FMLInitializationEvent ev) {
        super.init(ev);
        registerClientEvents();
        AngelicaClientCompat.registerRawTextSuppressor();
        AngelicaClientCompat.registerGlyphEffects();
    }

    @Override
    public void postInit(FMLPostInitializationEvent ev) {
        super.postInit(ev);
    }

    @Override
    public void loadComplete(FMLLoadCompleteEvent ev) {
        super.loadComplete(ev);
        GTNHLibTextCompat.claimTextPreprocessor();
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
                                  boolean repairAmpersands, boolean allowSignEditing, boolean enableHtmlFormat,
                                  String[] bannedSignPatterns) {
        remoteServerHasHexText = true;
        clientConfig.apply(universalAmpersand, chatAmpersands, signAmpersands, repairAmpersands, allowSignEditing,
            enableHtmlFormat, bannedSignPatterns);
    }

    @Override
    public boolean isRemoteHexTextPresent() {
        return remoteServerHasHexText;
    }

    @Override
    public void setRemoteServerHasHexText(boolean hasHexText) {
        this.remoteServerHasHexText = hasHexText;
    }

    private void registerClientEvents() {
        if (!clientEventsRegistered) {
            FMLCommonHandler.instance().bus().register(connectionEventHandler);
            clientEventsRegistered = true;
        }
    }
}
