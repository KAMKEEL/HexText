package kamkeel.hextext.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * Handles loading and syncing configuration values for HexText.
 */
public final class HexTextConfig {

    public static final String CATEGORY_EFFECTS = "effects";

    private static final float DEFAULT_RAINBOW_SPEED = 55.0f;
    private static final int DEFAULT_SHAKE_INTERVAL = 16;
    private static final int DEFAULT_IGNITE_INTERVAL = 120;

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
            "Milliseconds between ignite bright and dim states. Lower values blink faster."
        ), MIN_IGNITE_INTERVAL, MAX_IGNITE_INTERVAL);

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

    public static Configuration getConfiguration() {
        return configuration;
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
