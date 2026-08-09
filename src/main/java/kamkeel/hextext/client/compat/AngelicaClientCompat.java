package kamkeel.hextext.client.compat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.BooleanSupplier;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.api.HexTextApi;
import net.minecraft.client.Minecraft;
import kamkeel.hextext.client.render.FontRenderContext;
import kamkeel.hextext.common.compat.AngelicaCompatibility;
import kamkeel.hextext.common.util.ColorCodeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client-side wiring into Angelica when its font renderer replaces the vanilla pipeline. Hands
 * HexText's raw text state to Angelica as a conversion suppressor so edit GUIs (chat input,
 * signs, anvils) keep showing formatting codes literally, and registers HexText's ignite and
 * shake as per-glyph font effects so both keep their native behaviour and timing configs under
 * Angelica's batching renderer.
 *
 * <p>All Angelica access is reflective; on older Angelica builds each hook degrades independently
 * (raw editing shows converted colors, ignite renders static, shake renders as wave).
 */
public final class AngelicaClientCompat {

    private static final Logger LOGGER = LogManager.getLogger("HexText|AngelicaCompat");
    private static final String ANGELICA_COLOR_CODE_UTILS = "com.gtnewhorizons.angelica.client.font.ColorCodeUtils";
    private static final String ANGELICA_FONT_EFFECT_REGISTRY = "com.gtnewhorizons.angelica.client.font.FontEffectRegistry";
    private static final String ANGELICA_CUSTOM_GLYPH_EFFECT = "com.gtnewhorizons.angelica.client.font.CustomGlyphEffect";
    private static final String ANGELICA_CONFIG = "com.gtnewhorizons.angelica.config.AngelicaConfig";

    private static volatile boolean glyphEffectsRegistered;

    private static volatile Boolean ampersandCodesConverted;

    private AngelicaClientCompat() {
    }

    /**
     * Whether Angelica turns ampersand codes into its own grammar downstream.
     *
     * <p>Asked about the codes HexText does not own - Angelica's q and v - to decide
     * whether an editor should preview them. What the editor shows is a promise about
     * what the finished line will look like, and the only thing that keeps that
     * promise for a code somebody else converts is that they are actually going to
     * convert it. With the setting off the code stays text once sent, so previewing a
     * rainbow would be showing the writer something they are not going to get.</p>
     *
     * <p>Read once. It is a startup setting, and this is asked per token.</p>
     */
    public static boolean convertsAmpersandCodes() {
        final Boolean cached = ampersandCodesConverted;
        if (cached != null) {
            return cached;
        }
        boolean converts;
        try {
            converts = Class.forName(ANGELICA_CONFIG).getField("enableAmpersandConversion").getBoolean(null);
        } catch (ReflectiveOperationException | LinkageError ex) {
            // Nothing to read means nothing downstream is going to convert anything.
            converts = false;
            LOGGER.debug("Could not read Angelica's ampersand conversion setting", ex);
        }
        ampersandCodesConverted = converts;
        return converts;
    }

    /** Test hook. */
    static void setAmpersandCodesConverted(Boolean converted) {
        ampersandCodesConverted = converted;
    }

    public static void registerRawTextSuppressor() {
        if (!AngelicaCompatibility.isAngelicaFontRendererActive()) {
            return;
        }
        try {
            Class<?> colorCodeUtils = Class.forName(ANGELICA_COLOR_CODE_UTILS);
            Method setter = colorCodeUtils.getMethod("setConversionSuppressor", BooleanSupplier.class);
            setter.invoke(null, (BooleanSupplier) AngelicaClientCompat::shouldSuppressConversion);
            LOGGER.info("Registered HexText as Angelica's ampersand conversion suppressor "
                + "(raw editing, and wherever the server has not allowed ampersand codes).");
        } catch (ReflectiveOperationException | LinkageError ex) {
            LOGGER.info("Angelica does not expose a conversion suppressor; raw text editing will show converted colors.");
            LOGGER.debug("Suppressor registration failed", ex);
        }
    }

    /**
     * Whether Angelica should leave ampersands alone right now.
     *
     * <p>Two unrelated reasons, both answered here because Angelica only offers the
     * one hook.</p>
     *
     * <p>The first is an editor: a code being typed has to stay visible as the
     * characters it is made of, so nothing may convert it out from under the person
     * writing it.</p>
     *
     * <p>The second is the server's policy. Angelica is a client mod and its
     * ampersand conversion is a client setting, so a server cannot turn it off - a
     * player could switch colour codes on locally and use them where the server had
     * decided they were not allowed. HexText already learns the server's answer
     * through its own config sync, and holding conversion off is the one lever that
     * works from this side: it cannot force a setting, but it can decline to act on
     * one. Soft, and honest about which side owns what - a player who leaves the
     * server keeps their own setting untouched.</p>
     */
    static boolean shouldSuppressConversion() {
        if (FontRenderContext.isRawTextRendering()) {
            return true;
        }
        CommonProxy proxy = HexText.getActiveProxy();
        // No proxy yet means nothing has said otherwise, and the local config is the
        // only authority there is; converting is the standing behaviour.
        return proxy != null && !proxy.allowUniversalAmpersand();
    }

    /**
     * Claims the free {@code §i}/{@code §j} format codes in Angelica's font effect registry and
     * backs them with HexText's own ignite and shake math, so the translator can emit real ignite
     * and jitter instead of dropping or approximating them.
     */
    public static void registerGlyphEffects() {
        if (!AngelicaCompatibility.isAngelicaFontRendererActive()) {
            return;
        }
        try {
            Class<?> registry = Class.forName(ANGELICA_FONT_EFFECT_REGISTRY);
            Class<?> effectType = Class.forName(ANGELICA_CUSTOM_GLYPH_EFFECT);
            Method register = registry.getMethod("register", char.class, effectType);

            boolean igniteOk = Boolean.TRUE.equals(register.invoke(null, 'i', newEffectProxy(effectType, true)));
            boolean shakeOk = Boolean.TRUE.equals(register.invoke(null, 'j', newEffectProxy(effectType, false)));
            // HexText's rainbow rather than Angelica's. Angelica's §q reads a fixed
            // lookup table indexed by character, so it is a gradient that never moves;
            // HexText's is driven by the clock and its own speed setting, and cycles.
            // Mapping &g onto §q therefore traded an animated rainbow for a still one.
            rainbowRegistered = Boolean.TRUE.equals(
                register.invoke(null, RAINBOW_CODE, newRainbowProxy(effectType)));
            // Registered separately from the pair above, and only against an Angelica
            // whose effects can actually draw behind a glyph. The registry predates
            // that hook, so a build in between would accept the code and then paint
            // nothing - claiming a letter of a shared namespace to no purpose, and
            // leaving the translator emitting toggles for an effect that does not
            // exist. Asking the interface what it can do is a surer test than asking
            // Angelica what version it is.
            if (hasBackgroundHook(effectType)) {
                highlightRegistered = Boolean.TRUE.equals(
                    register.invoke(null, HIGHLIGHT_CODE, newHighlightProxy(effectType)));
                if (highlightRegistered) {
                    LOGGER.info("Registered HexText token highlighting as an Angelica font effect.");
                } else {
                    LOGGER.info("Angelica rejected the highlight code; edited codes draw without a wash.");
                }
            } else {
                LOGGER.info("This Angelica's font effects cannot draw a background; "
                    + "edited codes draw without a wash.");
            }
            glyphEffectsRegistered = igniteOk && shakeOk;
            if (glyphEffectsRegistered) {
                LOGGER.info("Registered HexText ignite and shake as Angelica font effects.");
            } else {
                LOGGER.warn("Angelica rejected the ignite/shake effect codes; falling back to approximations.");
            }
        } catch (ReflectiveOperationException | LinkageError ex) {
            LOGGER.info("Angelica does not expose a font effect registry; ignite and shake fall back to approximations.");
            LOGGER.debug("Font effect registration failed", ex);
        }
    }

    public static boolean areGlyphEffectsRegistered() {
        return glyphEffectsRegistered;
    }

    /** Test hook. */
    static void setGlyphEffectsRegistered(boolean registered) {
        glyphEffectsRegistered = registered;
    }

    private static Object newEffectProxy(Class<?> effectType, boolean ignite) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "transformColor": {
                    int argb = (Integer) args[0];
                    if (!ignite) {
                        return argb;
                    }
                    int rgb = HexTextApi.dynamicEffects()
                        .computeIgniteColor(Minecraft.getSystemTime(), argb & 0xFFFFFF);
                    return (argb & 0xFF000000) | (rgb & 0xFFFFFF);
                }
                case "offsetY":
                    return ignite ? 0.0f
                        : HexTextApi.dynamicEffects().computeShakeOffset(Minecraft.getSystemTime(), (Integer) args[0]);
                case "offsetX":
                    return 0.0f;
                case "backgroundColor":
                    return 0;
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return ignite ? "HexTextIgniteEffect" : "HexTextShakeEffect";
                default:
                    // Never null. Every hook on the interface returns a primitive, so
                    // a null here unboxes into a NullPointerException inside the glyph
                    // loop - once per glyph, on whichever mod happens to be drawing.
                    // A method this proxy has not heard of is one it does not act on.
                    return defaultFor(method.getReturnType());
            }
        };
        return Proxy.newProxyInstance(AngelicaClientCompat.class.getClassLoader(), new Class<?>[] { effectType }, handler);
    }

    /**
     * Whether this Angelica's {@code CustomGlyphEffect} can draw behind a glyph.
     *
     * <p>Capability rather than version. Every other hook here is guarded the same way
     * - by whether the class and method are there to call - which is what lets one
     * feature degrade without taking the rest of the compat down with it.</p>
     */
    private static boolean hasBackgroundHook(Class<?> effectType) {
        try {
            effectType.getMethod("backgroundColor", int.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return false;
        }
    }

    /** The harmless answer for a hook this proxy does not implement. */
    private static Object defaultFor(Class<?> returnType) {
        if (returnType == int.class) return 0;
        if (returnType == float.class) return 0.0f;
        if (returnType == double.class) return 0.0d;
        if (returnType == long.class) return 0L;
        if (returnType == boolean.class) return Boolean.FALSE;
        return null;
    }

    /** Free in both namespaces, and where HexText's own rainbow is published. */
    private static final char RAINBOW_CODE = 't';

    private static volatile boolean rainbowRegistered;

    public static boolean isRainbowRegistered() {
        return rainbowRegistered;
    }

    public static char rainbowCode() {
        return RAINBOW_CODE;
    }

    /** Test hook. */
    static void setRainbowRegistered(boolean registered) {
        rainbowRegistered = registered;
    }

    /**
     * Uses {@link Minecraft#getSystemTime()}, not wall clock, like the native pipeline.
     * The hue maths casts to float, and a millisecond epoch is far past what seven
     * significant digits can hold - both the per-glyph spread and the per-frame movement
     * round away, leaving a flat unchanging colour.
     */
    private static Object newRainbowProxy(Class<?> effectType) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "transformColor": {
                    int argb = (Integer) args[0];
                    boolean shadowPass = (Boolean) args[1];
                    int glyphIndex = (Integer) args[2];
                    int rgb = HexTextApi.dynamicEffects()
                        .computeRainbowColor(Minecraft.getSystemTime(), glyphIndex, 0);
                    // Darkened on the shadow pass. This hook is called twice per glyph
                    // and the second call arrives with the shadow's own colour already
                    // worked out; replacing it outright - which is what a rainbow does,
                    // unlike ignite, which only scales what it is handed - drew the
                    // shadow in full-brightness rainbow behind the letter and read as a
                    // smear rather than as a shadow.
                    if (shadowPass) {
                        rgb = ColorCodeUtils.calculateShadowColor(rgb);
                    }
                    return (argb & 0xFF000000) | (rgb & 0xFFFFFF);
                }
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return "HexTextRainbowEffect";
                default:
                    return defaultFor(method.getReturnType());
            }
        };
        return Proxy.newProxyInstance(AngelicaClientCompat.class.getClassLoader(),
            new Class<?>[] { effectType }, handler);
    }

    /**
     * The wash drawn behind a formatting code while it is being edited.
     *
     * <p>Angelica's renderer clears custom effects on a colour and on reset, and
     * toggles them on their own code, so this is turned on before the characters of a
     * token and off after them. The colour is the one HexText's own renderer uses for
     * a recognised code, so an editor looks the same whichever renderer is under
     * it.</p>
     */
    private static final int TOKEN_HIGHLIGHT_ARGB = 0x304080FF;
    /** Free in Angelica's namespace, and free in HexText's. */
    private static final char HIGHLIGHT_CODE = 'y';

    private static volatile boolean highlightRegistered;

    public static boolean isHighlightRegistered() {
        return highlightRegistered;
    }

    public static char highlightCode() {
        return HIGHLIGHT_CODE;
    }

    /** Test hook. */
    static void setHighlightRegistered(boolean registered) {
        highlightRegistered = registered;
    }

    private static Object newHighlightProxy(Class<?> effectType) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "transformColor":
                    return args[0];
                case "backgroundColor":
                    return TOKEN_HIGHLIGHT_ARGB;
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return "HexTextTokenHighlight";
                default:
                    return defaultFor(method.getReturnType());
            }
        };
        return Proxy.newProxyInstance(AngelicaClientCompat.class.getClassLoader(),
            new Class<?>[] { effectType }, handler);
    }
}
