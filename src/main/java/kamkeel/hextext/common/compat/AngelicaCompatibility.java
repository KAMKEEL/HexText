package kamkeel.hextext.common.compat;

import cpw.mods.fml.common.Loader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Detects whether Angelica is present with its font renderer mixin enabled so HexText can gracefully
 * back off from installing its own font renderer hooks.
 */
public final class AngelicaCompatibility {

    private static final Logger LOGGER = LogManager.getLogger("HexText|AngelicaCompat");
    private static final String ANGELICA_MOD_ID = "angelica";
    private static final String ANGELICA_FONT_RENDERER_MIXIN =
        "com.gtnewhorizons.angelica.mixins.early.angelica.fontrenderer.MixinFontRenderer";
    private static final String ANGELICA_MIXINS_ENUM = "com.gtnewhorizons.angelica.mixins.Mixins";
    private static final String ANGELICA_FONT_RENDERER_ENUM = "ANGELICA_FONT_RENDERER";
    private static final String ANGELICA_CONFIG_CLASS = "com.gtnewhorizons.angelica.config.AngelicaConfig";
    private static final String ANGELICA_FONT_CONFIG_FIELD = "enableFontRenderer";

    private static volatile Boolean cachedResult;

    private AngelicaCompatibility() {}

    /**
     * Returns {@code true} when Angelica's batching font renderer is expected to replace the vanilla
     * pipeline, meaning HexText runs in translation mode instead of hooking the glyph loop.
     */
    public static boolean isAngelicaFontRendererActive() {
        return shouldDisableHexTextFontRendererMixin();
    }

    /**
     * Returns {@code true} when HexText should skip applying its font renderer mixin because Angelica will
     * provide its own implementation.
     */
    public static boolean shouldDisableHexTextFontRendererMixin() {
        Boolean result = cachedResult;
        if (result != null) {
            return result;
        }
        result = detectAngelicaFontRendererMixin();
        cachedResult = result;
        return result;
    }

    private static boolean detectAngelicaFontRendererMixin() {
        if (!isAngelicaPresent()) {
            return false;
        }

        Boolean builderDecision = queryMixinBuilderState();
        if (builderDecision != null) {
            if (!builderDecision.booleanValue()) {
                LOGGER.info(
                    "Angelica font renderer mixin reported as disabled by builder state; keeping HexText mixin enabled.");
                return false;
            }
            LOGGER.info("Angelica font renderer mixin reported as active; disabling HexText font renderer mixin.");
            return true;
        }

        Boolean configDecision = readAngelicaFontRendererConfig();
        if (configDecision != null) {
            if (!configDecision.booleanValue()) {
                LOGGER.info(
                    "Angelica font renderer mixin disabled through configuration; keeping HexText mixin enabled.");
                return false;
            }
            LOGGER.info(
                "Angelica font renderer mixin enabled through configuration; disabling HexText font renderer mixin.");
            return true;
        }

        // Mixin restricts class loading for registered mixin packages, so this probe can fail even
        // when Angelica's font renderer is active. It stays as a last resort only.
        if (isClassPresent(ANGELICA_FONT_RENDERER_MIXIN)) {
            LOGGER.info("Angelica font renderer mixin class present; disabling HexText font renderer mixin.");
            return true;
        }

        LOGGER.info("Angelica detected but its font renderer state could not be determined; keeping HexText mixin enabled.");
        return false;
    }

    private static boolean isAngelicaPresent() {
        try {
            return Loader.isModLoaded(ANGELICA_MOD_ID);
        } catch (Throwable t) {
            // Expected, and not worth a stack trace on every launch. The only
            // caller that matters is getMixins(), which runs during early mixin
            // loading - long before FML has a mod list to consult, so the loader
            // answers with a NullPointerException rather than with "no". The
            // classpath is the one thing that can be asked this early, and it is
            // enough: a mod that is present has brought its classes with it.
            LOGGER.debug("Mod loader not ready for an Angelica presence check; asking the classpath", t);
            return isClassPresent("com.gtnewhorizons.angelica.AngelicaMod");
        }
    }

    private static Boolean queryMixinBuilderState() {
        try {
            Class<?> mixinsEnum = Class.forName(ANGELICA_MIXINS_ENUM, false, AngelicaCompatibility.class.getClassLoader());
            if (!mixinsEnum.isEnum()) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Enum<?> fontEntry = Enum.valueOf((Class<Enum>) mixinsEnum, ANGELICA_FONT_RENDERER_ENUM);
            Object builder = mixinsEnum.getMethod("getBuilder").invoke(fontEntry);
            if (builder == null) {
                return null;
            }

            Set<String> mixinNames = extractMixinNames(builder);
            if (!mixinNames.isEmpty() && !mixinNames.contains("angelica.fontrenderer.MixinFontRenderer")) {
                return Boolean.FALSE;
            }

            Boolean decision = evaluateBuilderApplicability(builder);
            if (decision != null) {
                return decision;
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            LOGGER.debug("Unable to query Angelica mixin builder state", ex);
        }
        return null;
    }

    /**
     * Collects the client mixin names registered on the builder. Recent GTNHMixins builders keep
     * them in the protected {@code clientClasses}/{@code commonClasses} list fields with no public
     * accessor, so the fields are read directly. An empty result means the layout is unknown, not
     * that no mixins are registered.
     */
    private static Set<String> extractMixinNames(Object builder) {
        Set<String> values = new HashSet<>();
        for (String fieldName : Arrays.asList("clientClasses", "commonClasses")) {
            Object raw = readFieldValue(builder, fieldName);
            if (raw instanceof Collection) {
                for (Object entry : (Collection<?>) raw) {
                    if (entry instanceof String) {
                        values.add((String) entry);
                    }
                }
            }
        }
        return values;
    }

    /**
     * Evaluates the builder's {@code applyIf} supplier, which for the font renderer entry is
     * Angelica's own {@code () -> AngelicaConfig.enableFontRenderer} gate. Running it delegates
     * the decision to exactly the condition Angelica applies its mixin with.
     */
    private static Boolean evaluateBuilderApplicability(Object builder) {
        Object supplier = readFieldValue(builder, "applyIf");
        if (supplier instanceof Supplier) {
            Object value = ((Supplier<?>) supplier).get();
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        }
        return null;
    }

    private static Object readFieldValue(Object instance, String fieldName) {
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(instance);
            } catch (NoSuchFieldException ignored) {
                // Field may be declared on a superclass; keep walking up.
            } catch (ReflectiveOperationException | LinkageError ex) {
                LOGGER.debug("Unable to read builder field {}", fieldName, ex);
                return null;
            }
        }
        return null;
    }

    private static Boolean readAngelicaFontRendererConfig() {
        try {
            Class<?> config = Class.forName(ANGELICA_CONFIG_CLASS, false, AngelicaCompatibility.class.getClassLoader());
            Field field = config.getField(ANGELICA_FONT_CONFIG_FIELD);
            if (field.getType() == boolean.class) {
                return field.getBoolean(null);
            }
        } catch (ReflectiveOperationException ex) {
            LOGGER.debug("Unable to query Angelica font renderer configuration", ex);
        }
        return null;
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, AngelicaCompatibility.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ex) {
            return false;
        }
    }
}
