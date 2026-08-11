package kamkeel.hextext.client.compat;

import com.gtnewhorizon.gtnhlib.util.font.FontRendering;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Registers HexText with GTNHLib's font layer. The slot holds one preprocessor, and the
 * width code reads its flags, so claiming it is what keeps measuring and drawing on the
 * same rules. Registered at loadComplete because the slot is last-write-wins and other
 * mods claim it during postInit.
 * <p>
 * Every GTNHLib reference lives behind this class, so it simply never loads when GTNHLib
 * is absent and HexText renders on its own.
 */
public final class GTNHLibTextCompat {

    private static final Logger LOGGER = LogManager.getLogger("HexText|GTNHLibCompat");

    private GTNHLibTextCompat() {
    }

    /** Call only through {@link #claimTextPreprocessor()}, which guards the class loading. */
    static void register() {
        FontRendering.setTextPreprocessor(new HexTextPreprocessor());
        LOGGER.info("Registered HexText with GTNHLib's font layer.");
    }

    public static void claimTextPreprocessor() {
        try {
            register();
        } catch (LinkageError ex) {
            LOGGER.info("GTNHLib's font layer is absent or too old; HexText measures text itself.");
            LOGGER.debug("Preprocessor registration failed", ex);
        }
    }
}
