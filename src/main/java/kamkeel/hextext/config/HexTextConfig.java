package kamkeel.hextext.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * Handles loading and syncing configuration values for HexText.
 */
public final class HexTextConfig {

    public static final String CATEGORY_EFFECTS = "effects";
    public static final String CATEGORY_SERVER = "server";

    private static final float DEFAULT_RAINBOW_SPEED = 3000.0f;
    private static final int DEFAULT_SHAKE_INTERVAL = 100;
    private static final int DEFAULT_IGNITE_INTERVAL = 100;

    private static final boolean DEFAULT_ENABLE_RGB_HTML_FORMAT = false;
    private static final boolean DEFAULT_ALLOW_SIGN_EDITING = true;
    private static final boolean DEFAULT_ALLOW_AMPERSAND = true;

    private static final float MIN_RAINBOW_SPEED = 1.0f;
    private static final int MIN_SHAKE_INTERVAL = 1;
    private static final int MIN_IGNITE_INTERVAL = 1;

    private static final int MAX_SHAKE_INTERVAL = 1000;
    private static final int MAX_IGNITE_INTERVAL = 1000;
    private static final float MAX_RAINBOW_SPEED = 5000.0f;

    private static Configuration configuration;

    private static float rainbowSpeed = DEFAULT_RAINBOW_SPEED;
    private static int shakeInterval = DEFAULT_SHAKE_INTERVAL;
    private static int igniteInterval = DEFAULT_IGNITE_INTERVAL;
    private static boolean enableRgbHtmlFormat = DEFAULT_ENABLE_RGB_HTML_FORMAT;
    private static boolean allowSignEditing = DEFAULT_ALLOW_SIGN_EDITING;
    private static boolean allowAmpersand = DEFAULT_ALLOW_AMPERSAND;

    private HexTextConfig() {
    }

    public static void init(File file) {
        if (configuration == null) {
            configuration = new Configuration(file);
        }
        sync();
    }

    public static void sync() {
        if (configuration == null) {
            return;
        }

        configuration.load();

        configuration.addCustomCategoryComment(CATEGORY_EFFECTS,
            "Timing controls for HexText's dynamic formatting effects.");
        configuration.addCustomCategoryComment(CATEGORY_SERVER,
            "Server-side behavioural toggles for HexText formatting and sign editing.");

        rainbowSpeed = clamp(configuration.getFloat(
            "rainbowCycleDurationMs",
            CATEGORY_EFFECTS,
            DEFAULT_RAINBOW_SPEED,
            MIN_RAINBOW_SPEED,
            MAX_RAINBOW_SPEED,
            "Milliseconds required for the rainbow effect to rotate through the full spectrum. Lower values move faster."
        ), MIN_RAINBOW_SPEED, MAX_RAINBOW_SPEED);

        shakeInterval = clamp(configuration.getInt(
            "shakeUpdateIntervalMs",
            CATEGORY_EFFECTS,
            DEFAULT_SHAKE_INTERVAL,
            MIN_SHAKE_INTERVAL,
            MAX_SHAKE_INTERVAL,
            "Milliseconds between shake offset updates. Lower values produce a more frantic shake."
        ), MIN_SHAKE_INTERVAL, MAX_SHAKE_INTERVAL);

        igniteInterval = clamp(configuration.getInt(
            "igniteBlinkIntervalMs",
            CATEGORY_EFFECTS,
            DEFAULT_IGNITE_INTERVAL,
            MIN_IGNITE_INTERVAL,
            MAX_IGNITE_INTERVAL,
            "Milliseconds for ignite to fade from bright to dim and back again. Lower values animate faster."
        ), MIN_IGNITE_INTERVAL, MAX_IGNITE_INTERVAL);

        enableRgbHtmlFormat = configuration.getBoolean(
            "enableRgbHtmlFormat",
            CATEGORY_SERVER,
            DEFAULT_ENABLE_RGB_HTML_FORMAT,
            "Allow the <RRGGBB> and </RRGGBB> HTML-style colour tags to be parsed."
        );

        allowSignEditing = configuration.getBoolean(
            "allowSignEditing",
            CATEGORY_SERVER,
            DEFAULT_ALLOW_SIGN_EDITING,
            "Permit players to open the sign editor by right-clicking with an empty hand."
        );

        allowAmpersand = configuration.getBoolean(
            "allowAmpersandFormatting",
            CATEGORY_SERVER,
            DEFAULT_ALLOW_AMPERSAND,
            "Enable & as an alternative to the section sign when entering formatting codes."
        );

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static float getRainbowSpeed() {
        return rainbowSpeed;
    }

    public static int getShakeInterval() {
        return shakeInterval;
    }

    public static int getIgniteInterval() {
        return igniteInterval;
    }

    public static boolean isRgbHtmlFormatEnabled() {
        return enableRgbHtmlFormat;
    }

    public static boolean isSignEditingAllowed() {
        return allowSignEditing;
    }

    public static boolean isAmpersandAllowed() {
        return allowAmpersand;
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static void setEnableRgbHtmlFormat(boolean enabled) {
        enableRgbHtmlFormat = enabled;
    }

    public static void setAllowSignEditing(boolean allowed) {
        allowSignEditing = allowed;
    }

    public static void setAllowAmpersand(boolean allowed) {
        allowAmpersand = allowed;
    }

    public static void resetToDefaults() {
        rainbowSpeed = DEFAULT_RAINBOW_SPEED;
        shakeInterval = DEFAULT_SHAKE_INTERVAL;
        igniteInterval = DEFAULT_IGNITE_INTERVAL;
        enableRgbHtmlFormat = DEFAULT_ENABLE_RGB_HTML_FORMAT;
        allowSignEditing = DEFAULT_ALLOW_SIGN_EDITING;
        allowAmpersand = DEFAULT_ALLOW_AMPERSAND;
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

}
