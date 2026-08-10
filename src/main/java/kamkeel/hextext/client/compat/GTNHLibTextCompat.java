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
 * Claims GTNHLib's text preprocessor slot when HexText renders text itself. Other mods
 * fill it with fallbacks that flatten hex and gradients before the renderer sees them,
 * so an identity function is registered instead. Registered at loadComplete, after every
 * postInit, since the slot is last-write-wins. Skipped when Angelica owns the renderer.
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
                    // Live, not captured: width code asks this to decide whether
                    // &-pairs are zero-width, and the config can change.
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
