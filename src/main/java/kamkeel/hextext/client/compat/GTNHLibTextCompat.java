package kamkeel.hextext.client.compat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.common.compat.AngelicaCompatibility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Claims GTNHLib's text preprocessor slot when HexText is rendering text itself.
 *
 * <p>The slot holds one function that other mods run over every string before the
 * font renderer sees it. Angelica fills it with its own colour conversion, and
 * Hodgepodge - when Angelica is absent - fills it with a fallback that rewrites
 * extended colours down to the sixteen vanilla ones: hex to the nearest colour
 * code, gradients to a single flat colour. Reasonable when nothing better is
 * installed; with HexText present it destroys exactly the codes HexText's own
 * renderer would have drawn in full, before that renderer ever sees them.</p>
 *
 * <p>So HexText puts an identity function there. Text then reaches the vanilla
 * pipeline untouched, where HexText's renderer reads the codes natively - and an
 * edit box keeps showing the characters that were typed, which the rewriting
 * preprocessor also broke. The claim happens at loadComplete because Hodgepodge
 * registers its fallback at postInit, and every mod's postInit has run by then -
 * last write wins and this has to be it.</p>
 *
 * <p>When Angelica's font renderer is active this does nothing: the slot is
 * Angelica's, HexText translates its grammar to Angelica's instead, and the
 * suppressor already governs the ampersand conversion.</p>
 */
public final class GTNHLibTextCompat {

    private static final Logger LOGGER = LogManager.getLogger("HexText|GTNHLibCompat");
    private static final String FONT_RENDERING = "com.gtnewhorizon.gtnhlib.util.font.FontRendering";
    private static final String TEXT_PREPROCESSOR = "com.gtnewhorizon.gtnhlib.util.font.FontRendering$TextPreprocessor";

    private GTNHLibTextCompat() {
    }

    public static void claimTextPreprocessor() {
        if (AngelicaCompatibility.isAngelicaFontRendererActive()) {
            return;
        }
        try {
            Class<?> fontRendering = Class.forName(FONT_RENDERING);
            Class<?> preprocessorType = Class.forName(TEXT_PREPROCESSOR);
            Method setter = fontRendering.getMethod("setTextPreprocessor", Function.class);
            setter.invoke(null, newIdentityPreprocessor(preprocessorType));
            LOGGER.info("Claimed GTNHLib's text preprocessor; HexText reads its own codes "
                + "and no fallback rewrites them first.");
        } catch (ReflectiveOperationException | LinkageError ex) {
            LOGGER.info("GTNHLib's text preprocessor slot is not available; "
                + "another mod's fallback may rewrite extended colours before HexText reads them.");
            LOGGER.debug("Preprocessor registration failed", ex);
        }
    }

    private static Object newIdentityPreprocessor(Class<?> preprocessorType) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "apply":
                    return args[0];
                case "handlesAmpCodes": {
                    // Live, not captured: width code asks this to decide whether &-pairs
                    // are zero-width, and the answer is whatever the config says the
                    // renderer will do with them right now.
                    CommonProxy proxy1 = HexText.getActiveProxy();
                    return proxy1 != null && proxy1.allowUniversalAmpersand();
                }
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return "HexTextIdentityPreprocessor";
                default:
                    return null;
            }
        };
        return Proxy.newProxyInstance(GTNHLibTextCompat.class.getClassLoader(),
            new Class<?>[] { preprocessorType }, handler);
    }
}
