package kamkeel.hextext.client.config;

import kamkeel.hextext.config.HexTextConfig;

/**
 * Stores the subset of configuration options that are governed by the server while playing on a
 * remote world.
 * <p>
 * Original values are captured at boot from the local config file and remain immutable.
 * Current values start as copies of the originals and can be overridden by server sync packets.
 * On disconnect, current values are reset back to the originals.
 */
public class ClientConfig {

    // Original values from local config (set once at boot, never modified)
    private final boolean originalUniversalAmpersand;
    private final boolean originalChatAmpersands;
    private final boolean originalSignAmpersands;
    private final boolean originalRepairAmpersands;
    private final boolean originalAllowSignEditing;
    private final boolean originalEnableHtmlFormat;

    // Current values (can be modified by server sync)
    private boolean universalAmpersand;
    private boolean chatAmpersands;
    private boolean signAmpersands;
    private boolean repairAmpersands;
    private boolean allowSignEditing;
    private boolean enableHtmlFormat;

    public ClientConfig() {
        // Capture original values from local config at boot
        originalUniversalAmpersand = HexTextConfig.isUniversalAmpersandEnabled();
        originalChatAmpersands = HexTextConfig.isChatAmpersandConversionEnabled();
        originalSignAmpersands = HexTextConfig.isSignAmpersandConversionEnabled();
        originalRepairAmpersands = HexTextConfig.isRepairAmpersandConversionEnabled();
        originalAllowSignEditing = HexTextConfig.isSignEditingAllowed();
        originalEnableHtmlFormat = HexTextConfig.isRgbHtmlFormatEnabled();

        // Initialize current values to originals
        resetToOriginal();
    }

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

    /**
     * Applies server-synced configuration values, overriding the current values.
     */
    public void apply(boolean universalAmpersand, boolean chatAmpersands, boolean signAmpersands,
        boolean repairAmpersands, boolean allowSignEditing, boolean enableHtmlFormat) {
        this.universalAmpersand = universalAmpersand;
        this.chatAmpersands = chatAmpersands;
        this.signAmpersands = signAmpersands;
        this.repairAmpersands = repairAmpersands;
        this.allowSignEditing = allowSignEditing;
        this.enableHtmlFormat = enableHtmlFormat;
    }

    /**
     * Resets current values back to the original local config values captured at boot.
     * Called when disconnecting from a server.
     */
    public void resetToOriginal() {
        universalAmpersand = originalUniversalAmpersand;
        chatAmpersands = originalChatAmpersands;
        signAmpersands = originalSignAmpersands;
        repairAmpersands = originalRepairAmpersands;
        allowSignEditing = originalAllowSignEditing;
        enableHtmlFormat = originalEnableHtmlFormat;
    }
}
