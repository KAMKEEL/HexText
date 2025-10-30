package kamkeel.hextext.client.config;

import kamkeel.hextext.config.HexTextConfig;

/**
 * Stores the subset of configuration options that are governed by the server while playing on a
 * remote world.
 */
public class ClientConfig {

    private boolean allowAmpersand = HexTextConfig.isAmpersandAllowed();
    private boolean allowSignEditing = HexTextConfig.isSignEditingAllowed();
    private boolean enableHtmlFormat = HexTextConfig.isRgbHtmlFormatEnabled();

    public boolean allowAmpersand() {
        return allowAmpersand;
    }

    public boolean allowSignEditing() {
        return allowSignEditing;
    }

    public boolean enableHtmlFormat() {
        return enableHtmlFormat;
    }

    public void apply(boolean allowAmpersand, boolean allowSignEditing, boolean enableHtmlFormat) {
        this.allowAmpersand = allowAmpersand;
        this.allowSignEditing = allowSignEditing;
        this.enableHtmlFormat = enableHtmlFormat;
    }

    public void resetToLocalConfig() {
        apply(
            HexTextConfig.isAmpersandAllowed(),
            HexTextConfig.isSignEditingAllowed(),
            HexTextConfig.isRgbHtmlFormatEnabled()
        );
    }
}
