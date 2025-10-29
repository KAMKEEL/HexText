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
        return FormattedTextMetrics.calculateMaxLineWidth(text, rawMode,
            character -> getCharWidth(renderer, character, rawMode), 0.0f, 1.0f);
    }

    public static int computeLineBreakIndex(FontRenderer renderer, String text, int maxWidth, boolean rawMode) {
        return FormattedTextMetrics.computeLineBreakIndex(text, maxWidth, rawMode,
            character -> getCharWidth(renderer, character, rawMode), 0.0f, 1.0f);
    }

    public static String trimStringFromEnd(FontRenderer renderer, String text, int width, boolean rawMode) {
        if (text == null || text.isEmpty()) {
            return finalizeResult("", rawMode);
        }

        float currentWidth = 0.0f;
        int firstSafePosition = text.length();
        boolean bold = false;

        for (int index = text.length() - 1; index >= 0; ) {
            char chr = text.charAt(index);

            if (!rawMode) {
                if (index >= 6 && text.charAt(index - 6) == '&' && ColorCodeUtils.isValidHexString(text, index - 5)) {
                    index -= 7;
                    firstSafePosition = index + 1;
                    bold = false;
                    continue;
                }

                if (index >= 1 && text.charAt(index - 1) == 167) {
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

                if (index >= 7 && text.charAt(index - 7) == '<' && text.charAt(index) == '>'
                    && ColorCodeUtils.isValidHexString(text, index - 6)) {
                    index -= 8;
                    firstSafePosition = index + 1;
                    bold = false;
                    continue;
                }

                if (index >= 8 && text.charAt(index - 8) == '<' && text.charAt(index - 7) == '/'
                    && text.charAt(index) == '>' && ColorCodeUtils.isValidHexString(text, index - 6)) {
                    index -= 9;
                    firstSafePosition = index + 1;
                    bold = false;
                    continue;
                }

                if (index >= 1 && text.charAt(index - 1) == '&') {
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
                return finalizeResult(text.substring(index + 1), rawMode);
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
                return finalizeResult(text.substring(firstSafePosition), rawMode);
            }

            currentWidth = nextWidth;
            firstSafePosition = index;
            index--;
        }

        return finalizeResult(text, rawMode);
    }

    public static String wrapFormattedString(FontRenderer renderer, String text, int wrapWidth, boolean rawMode) {
        if (text == null || text.isEmpty()) {
            return finalizeResult("", rawMode);
        }

        int breakPoint = computeLineBreakIndex(renderer, text, wrapWidth, rawMode);
        if (breakPoint >= text.length()) {
            return finalizeResult(text, rawMode);
        }

        String firstPart = text.substring(0, breakPoint);
        char breakChar = text.charAt(breakPoint);
        boolean skipChar = breakChar == ' ' || breakChar == '\n';

        String remainder = StringUtils.extractFormatFromString(firstPart)
            + text.substring(breakPoint + (skipChar ? 1 : 0));

        if (remainder.length() == text.length()) {
            return finalizeResult(firstPart + "\n" + remainder, rawMode);
        }

        String result = firstPart + "\n" + wrapFormattedString(renderer, remainder, wrapWidth, rawMode);
        return finalizeResult(result, rawMode);
    }

    public static String convertLegacyFormattingCodes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder builder = null;
        int length = text.length();

        for (int index = 0; index < length; index++) {
            char current = text.charAt(index);

            if (current == '&') {
                if (index + 7 <= length && ColorCodeUtils.isValidHexString(text, index + 1)) {
                    if (builder != null) {
                        builder.append(text, index, index + 7);
                    }
                    index += 6;
                    continue;
                }

                if (index + 1 < length) {
                    char next = text.charAt(index + 1);
                    if (ColorCodeUtils.isFormattingCode(next)) {
                        if (builder == null) {
                            builder = new StringBuilder(length);
                            builder.append(text, 0, index);
                        }
                        builder.append('§').append(next);
                        index++;
                        continue;
                    }
                }
            }

            if (builder != null) {
                builder.append(current);
            }
        }

        return builder == null ? text : builder.toString();
    }

    private static String finalizeResult(String result, boolean rawMode) {
        if (result == null || result.isEmpty() || rawMode) {
            return result;
        }
        return convertLegacyFormattingCodes(result);
    }
}
