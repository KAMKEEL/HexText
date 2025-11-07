package kamkeel.hextext.common.compat;

import cpw.mods.fml.common.Loader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
    private static final String MIXIN_BUILDER_CLASS = "com.gtnewhorizon.gtnhmixins.builders.MixinBuilder";
    private static final String ANGELICA_CONFIG_CLASS = "com.gtnewhorizons.angelica.config.AngelicaConfig";
    private static final String ANGELICA_FONT_CONFIG_FIELD = "enableFontRenderer";

    private static volatile Boolean cachedResult;

    private AngelicaCompatibility() {}

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

        if (!isClassPresent(ANGELICA_FONT_RENDERER_MIXIN)) {
            LOGGER.info("Angelica detected but no font renderer mixin class present; keeping HexText mixin enabled.");
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

        LOGGER.warn(
            "Angelica detected but unable to determine font renderer mixin state; disabling HexText mixin as a safeguard.");
        return true;
    }

    private static boolean isAngelicaPresent() {
        try {
            return Loader.isModLoaded(ANGELICA_MOD_ID);
        } catch (Throwable t) {
            LOGGER.warn("Failed to query Forge mod loader for Angelica presence", t);
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

            Class<?> builderClass = Class.forName(MIXIN_BUILDER_CLASS, false, builder.getClass().getClassLoader());

            Set<String> mixinNames = extractMixinNames(builder, builderClass);
            if (!mixinNames.contains("angelica.fontrenderer.MixinFontRenderer")) {
                return Boolean.FALSE;
            }

            Boolean decision = evaluateBuilderApplicability(builder, builderClass);
            if (decision != null) {
                return decision;
            }
        } catch (ReflectiveOperationException | LinkageError ex) {
            LOGGER.debug("Unable to query Angelica mixin builder state", ex);
        }
        return null;
    }

    private static Set<String> extractMixinNames(Object builder, Class<?> builderClass)
        throws ReflectiveOperationException {
        Object raw = null;
        try {
            raw = builderClass.getMethod("getClientMixins").invoke(builder);
        } catch (NoSuchMethodException ignored) {
            // Fall back to a generic accessor below.
        }

        if (raw == null) {
            for (String candidate : Arrays.asList("clientMixins", "getClientMixinClasses", "getMixins")) {
                try {
                    raw = builderClass.getMethod(candidate).invoke(builder);
                    break;
                } catch (NoSuchMethodException ignored) {
                    // Try next method name.
                }
            }
        }

        if (raw instanceof String[]) {
            return new HashSet<>(Arrays.asList((String[]) raw));
        }
        if (raw instanceof Collection) {
            Collection<?> collection = (Collection<?>) raw;
            Set<String> values = new HashSet<>();
            for (Object entry : collection) {
                if (entry instanceof String) {
                    values.add((String) entry);
                }
            }
            return values;
        }
        return Collections.emptySet();
    }

    private static Boolean evaluateBuilderApplicability(Object builder, Class<?> builderClass)
        throws ReflectiveOperationException {
        for (String candidate : Arrays.asList("shouldApply", "shouldApplyMixins", "isEnabled")) {
            try {
                Object value = builderClass.getMethod(candidate).invoke(builder);
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
            } catch (NoSuchMethodException ignored) {
                // Try next method name.
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
