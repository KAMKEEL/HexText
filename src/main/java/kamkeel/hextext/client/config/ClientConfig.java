package kamkeel.hextext.client.config;

import kamkeel.hextext.config.HexTextConfig;

/**
 * Stores the subset of configuration options that are governed by the server while playing on a
 * remote world.
 */
public class ClientConfig {

    private boolean universalAmpersand = HexTextConfig.isUniversalAmpersandEnabled();
    private boolean chatAmpersands = HexTextConfig.isChatAmpersandConversionEnabled();
    private boolean signAmpersands = HexTextConfig.isSignAmpersandConversionEnabled();
    private boolean repairAmpersands = HexTextConfig.isRepairAmpersandConversionEnabled();
    private boolean allowSignEditing = HexTextConfig.isSignEditingAllowed();
    private boolean enableHtmlFormat = HexTextConfig.isRgbHtmlFormatEnabled();

    public boolean allowUniversalAmpersand() {
        return universalAmpersand;
    }

    public boolean convertAmpersandsInChat() {
        return chatAmpersands;
    }

    public boolean convertAmpersandsOnSigns() {
        return signAmpersands;
    }

    public boolean convertAmpersandsInRepairs() {
        return repairAmpersands;
    }

    public boolean allowSignEditing() {
        return allowSignEditing;
    }

    public boolean enableHtmlFormat() {
        return enableHtmlFormat;
    }

    public void apply(boolean universalAmpersand, boolean chatAmpersands, boolean signAmpersands,
        boolean repairAmpersands, boolean allowSignEditing, boolean enableHtmlFormat) {
        this.universalAmpersand = universalAmpersand;
        this.chatAmpersands = chatAmpersands;
        this.signAmpersands = signAmpersands;
        this.repairAmpersands = repairAmpersands;
        this.allowSignEditing = allowSignEditing;
        this.enableHtmlFormat = enableHtmlFormat;
    }

    public void resetToLocalConfig() {
        apply(
            HexTextConfig.isUniversalAmpersandEnabled(),
            HexTextConfig.isChatAmpersandConversionEnabled(),
            HexTextConfig.isSignAmpersandConversionEnabled(),
            HexTextConfig.isRepairAmpersandConversionEnabled(),
            HexTextConfig.isSignEditingAllowed(),
            HexTextConfig.isRgbHtmlFormatEnabled()
        );
    }
}
