package kamkeel.hextext.client;

import kamkeel.hextext.util.ColorCodeUtils;
import kamkeel.hextext.util.StringUtils;
import net.minecraft.client.gui.FontRenderer;

/**
 * Shared font renderer helpers extracted from the mixin implementation.
 */
public final class FontRendererUtils {

    private FontRendererUtils() {}

    public static float getCharWidth(FontRenderer renderer, char chr, boolean rawMode) {
        if (chr == 167) {
            return 0.0f;
        }
        return renderer.getCharWidth(chr);
    }

    public static float calculateMaxLineWidth(FontRenderer renderer, String text, boolean rawMode) {
        String normalized = normalizeTextForMetrics(text, rawMode);
        return FormattedTextMetrics.calculateMaxLineWidth(normalized, rawMode,
            character -> getCharWidth(renderer, character, rawMode), 0.0f, 1.0f);
    }

    public static int computeLineBreakIndex(FontRenderer renderer, String text, int maxWidth, boolean rawMode) {
        String normalized = normalizeTextForMetrics(text, rawMode);
        return FormattedTextMetrics.computeLineBreakIndex(normalized, maxWidth, rawMode,
            character -> getCharWidth(renderer, character, rawMode), 0.0f, 1.0f);
    }

    public static String trimStringFromEnd(FontRenderer renderer, String text, int width, boolean rawMode) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String normalized = normalizeTextForMetrics(text, rawMode);
        float currentWidth = 0.0f;
        int firstSafePosition = text.length();
        boolean bold = false;

        for (int index = text.length() - 1; index >= 0; ) {
            char chr = normalized.charAt(index);

            if (!rawMode) {
                if (index >= 6 && normalized.charAt(index - 6) == '&' && ColorCodeUtils.isValidHexString(normalized, index - 5)) {
                    index -= 7;
                    firstSafePosition = index + 1;
                    bold = false;
                    continue;
                }

                if (index >= 1 && normalized.charAt(index - 1) == 167) {
                    char fmt = Character.toLowerCase(chr);
                    if (fmt == 'l') {
                        bold = true;
                    } else if (fmt == 'r' || ColorCodeUtils.isMinecraftColorCode(fmt)) {
                        bold = false;
                    }
                    index -= 2;
                    firstSafePosition = index + 1;
                    continue;
                }

                if (index >= 7 && normalized.charAt(index - 7) == '<' && normalized.charAt(index) == '>'
                    && ColorCodeUtils.isValidHexString(normalized, index - 6)) {
                    index -= 8;
                    firstSafePosition = index + 1;
                    bold = false;
                    continue;
                }

                if (index >= 8 && normalized.charAt(index - 8) == '<' && normalized.charAt(index - 7) == '/'
                    && normalized.charAt(index) == '>' && ColorCodeUtils.isValidHexString(normalized, index - 6)) {
                    index -= 9;
                    firstSafePosition = index + 1;
                    bold = false;
                    continue;
                }

                if (index >= 1 && normalized.charAt(index - 1) == '&') {
                    char fmt = Character.toLowerCase(chr);
                    if (ColorCodeUtils.isFormattingCode(fmt)) {
                        if (fmt == 'l') {
                            bold = true;
                        } else if (fmt == 'r' || ColorCodeUtils.isMinecraftColorCode(fmt)) {
                            bold = false;
                        }
                        index -= 2;
                        firstSafePosition = index + 1;
                        continue;
                    }
                }
            }

            if (chr == '\n') {
                return text.substring(index + 1);
            }

            float glyphWidth = getCharWidth(renderer, chr, rawMode);
            if (glyphWidth < 0.0f) {
                glyphWidth = 0.0f;
            }

            float nextWidth = currentWidth + glyphWidth;
            if (bold && glyphWidth > 0.0f) {
                nextWidth += 1.0f;
            }

            if (nextWidth > width) {
                return text.substring(firstSafePosition);
            }

            currentWidth = nextWidth;
            firstSafePosition = index;
            index--;
        }

        return text;
    }

    public static String wrapFormattedString(FontRenderer renderer, String text, int wrapWidth, boolean rawMode) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        int breakPoint = computeLineBreakIndex(renderer, text, wrapWidth, rawMode);
        if (breakPoint >= text.length()) {
            return text;
        }

        String normalized = normalizeTextForMetrics(text, rawMode);
        String firstPart = text.substring(0, breakPoint);
        char breakChar = normalized.charAt(breakPoint);
        boolean skipChar = breakChar == ' ' || breakChar == '\n';

        String remainder = StringUtils.extractFormatFromString(firstPart)
            + text.substring(breakPoint + (skipChar ? 1 : 0));

        if (remainder.length() == text.length()) {
            return firstPart + "\n" + remainder;
        }

        return firstPart + "\n" + wrapFormattedString(renderer, remainder, wrapWidth, rawMode);
    }

    private static String normalizeTextForMetrics(String text, boolean rawMode) {
        if (text == null || rawMode) {
            return text;
        }
        return StringUtils.normalizeLegacyFormattingCodes(text);
    }
}
