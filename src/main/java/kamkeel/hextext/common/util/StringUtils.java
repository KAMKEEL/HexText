package kamkeel.hextext.common.util;

import java.util.ArrayDeque;

/**
 * String-related helpers for dealing with Minecraft formatting codes.
 */
public final class StringUtils {

    public static char SECTION_SIGN = '§';

    private StringUtils() {
    }

    public static String normalizeForRawDisplay(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder builder = null;

        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);

            if (current == 167) {
                if (builder == null) {
                    builder = new StringBuilder(text.length() + 8);
                    builder.append(text, 0, i);
                }

                if (i + 1 < text.length()) {
                    builder.append('&');
                    builder.append(text.charAt(++i));
                } else {
                    builder.append('§');
                }
                continue;
            }

            if (builder != null) {
                builder.append(current);
            }
        }

        return builder == null ? text : builder.toString();
    }

    /**
     * Replaces recognised HexText ampersand formatting codes (e.g. {@code &a}, {@code &#abcdef}) with
     * section sign equivalents. Non-formatting ampersands are left untouched.
     *
     * @param text input containing potential ampersand formatting tokens
     * @return the input with recognised tokens normalised to use section signs
     */
    public static String convertAmpersandsToSectionSigns(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder builder = null;
        int index = 0;
        while (index < text.length()) {
            char current = text.charAt(index);

            if (current == '&') {
                int codeLen = ColorCodeUtils.detectAmpersandFormattingCodeLength(text, index);
                if (codeLen == 2 || codeLen == 8) {
                    // An escaped code is the reader's to see, not this converter's to
                    // read. The pair goes through untouched - backslash and all - and
                    // whichever renderer draws it knows the escape and shows the bare
                    // code. Converting it anyway painted the text and left the
                    // backslash standing in front of a colour it never asked for.
                    if (index > 0 && text.charAt(index - 1) == '\\') {
                        if (builder != null) {
                            builder.append(text, index, index + codeLen);
                        }
                        index += codeLen;
                        continue;
                    }

                    if (builder == null) {
                        builder = new StringBuilder(text.length());
                        builder.append(text, 0, index);
                    }

                    builder.append(SECTION_SIGN);
                    builder.append(text, index + 1, index + codeLen);
                    index += codeLen;
                    continue;
                }
            }

            if (builder != null) {
                builder.append(current);
            }
            index++;
        }

        return builder == null ? text : builder.toString();
    }

    /**
     * Converts recognised section sign formatting codes back to their ampersand equivalents. This is
     * primarily used when presenting stored text to editing interfaces where the user expects to work
     * with ampersand tokens.
     *
     * @param text input potentially containing section sign formatting tokens
     * @return the provided text with recognised section sign tokens rewritten to use ampersands
     */
    public static String convertSectionSignsToAmpersands(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder builder = null;
        int index = 0;
        ColorCodeUtils.FormattingEnvironment env = ColorCodeUtils.captureFormattingEnvironment(false);

        while (index < text.length()) {
            char current = text.charAt(index);

            if (current == SECTION_SIGN) {
                int codeLen = ColorCodeUtils.detectColorCodeLengthIgnoringRaw(text, index, env);
                if (codeLen == 2 || codeLen == 8) {
                    if (builder == null) {
                        builder = new StringBuilder(text.length());
                        builder.append(text, 0, index);
                    }

                    builder.append('&');
                    builder.append(text, index + 1, Math.min(index + codeLen, text.length()));
                    index += codeLen;
                    continue;
                }
            }

            if (builder != null) {
                builder.append(current);
            }
            index++;
        }

        return builder == null ? text : builder.toString();
    }

    public static String extractFormatFromString(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        String currentColorCode = null;
        StringBuilder styleCodes = new StringBuilder();
        ArrayDeque<String> colorStack = new ArrayDeque<>();
        ColorCodeUtils.FormattingEnvironment env = ColorCodeUtils.captureFormattingEnvironment(false);

        for (int i = 0; i < str.length(); ) {
            // The whole token, before its pieces can be read one by one: the two
            // hexes inside would each land as a flat colour and clear the styles,
            // leaving a wrapped gradient line flat in its end colour and unbold.
            // The token keeps styles for the same reason the renderer now does.
            int gradientLen = ColorCodeUtils.gradientTokenLength(str, i);
            if (gradientLen > 0) {
                currentColorCode = str.substring(i, i + gradientLen);
                colorStack.clear();
                i += gradientLen;
                continue;
            }

            int codeLen = ColorCodeUtils.detectColorCodeLengthIgnoringRaw(str, i, env);

            if (codeLen > 0) {
                char firstChar = str.charAt(i);
                String code = str.substring(i, i + codeLen);

                if (codeLen == 8 && (firstChar == '&' || firstChar == 167)) {
                    currentColorCode = code;
                    colorStack.clear();
                    styleCodes.setLength(0);
                } else if (codeLen == 8 && firstChar == '<') {
                    if (currentColorCode != null) {
                        colorStack.push(currentColorCode);
                    }
                    currentColorCode = code;
                    styleCodes.setLength(0);
                } else if (codeLen == 9 && firstChar == '<') {
                    currentColorCode = colorStack.isEmpty() ? null : colorStack.pop();
                    styleCodes.setLength(0);
                } else if (codeLen == 2) {
                    char fmt = Character.toLowerCase(str.charAt(i + 1));

                    if (ColorCodeUtils.isMinecraftColorCode(fmt) || fmt == 'g') {
                        currentColorCode = code;
                        colorStack.clear();
                        styleCodes.setLength(0);
                    } else if (ColorCodeUtils.isResetCode(fmt)) {
                        currentColorCode = null;
                        colorStack.clear();
                        styleCodes.setLength(0);
                    } else if (ColorCodeUtils.isStyleCode(fmt) || ColorCodeUtils.isEffectCode(fmt)) {
                        styleCodes.append(code);
                    }
                }

                i += codeLen;
                continue;
            }

            i++;
        }

        StringBuilder result = new StringBuilder();
        if (currentColorCode != null) {
            result.append(currentColorCode);
        }
        if (styleCodes.length() > 0) {
            result.append(styleCodes);
        }

        return result.toString();
    }

    public static String stripColorCodes(CharSequence input) {
        if (input == null) {
            return null;
        }

        return stripTokens(input, (text, start, codeLen) -> true);
    }

    /**
     * Removes all recognised formatting tokens from the provided text. This is an alias for
     * {@link #stripColorCodes(CharSequence)} that reads more clearly in contexts where any
     * non-textual extras should be discarded.
     */
    public static String stripExtras(CharSequence input) {
        return stripColorCodes(input);
    }

    /**
     * Removes colour-related formatting codes while preserving styles such as bold/italic.
     */
    public static String stripHexColors(CharSequence input) {
        if (input == null) {
            return null;
        }

        return stripTokens(input, ColorCodeUtils::isHexColorToken);
    }

    /**
     * Removes standard Minecraft-style colour codes (e.g. {@code &a}, {@code §c}) while leaving
     * hex colour tokens and style/effect markers untouched.
     */
    public static String stripColors(CharSequence input) {
        if (input == null) {
            return null;
        }

        return stripTokens(input, ColorCodeUtils::isStandardColorToken);
    }

    /**
     * Removes style/effect formatting codes (bold, italic, rainbow etc.) while leaving colours
     * untouched.
     */
    public static String stripStyles(CharSequence input) {
        if (input == null) {
            return null;
        }

        return stripTokens(input, ColorCodeUtils::isStyleOrEffectToken);
    }

    /**
     * @return {@code true} when the input contains any formatting codes understood by HexText.
     */
    public static boolean containsFormattingCodes(CharSequence input) {
        return ColorCodeUtils.containsFormattingCodes(input);
    }

    private static String stripTokens(CharSequence input, TokenClassifier classifier) {
        if (input == null) {
            return null;
        }

        StringBuilder builder = null;
        int index = 0;
        ColorCodeUtils.FormattingEnvironment env = ColorCodeUtils.captureFormattingEnvironment(false);

        while (index < input.length()) {
            int codeLen = ColorCodeUtils.detectColorCodeLengthIgnoringRaw(input, index, env);
            if (codeLen > 0) {
                if (classifier.shouldStrip(input, index, codeLen)) {
                    if (builder == null) {
                        builder = new StringBuilder(input.length());
                        builder.append(input, 0, index);
                    }
                    index += codeLen;
                    continue;
                }

                if (builder != null) {
                    builder.append(input, index, index + codeLen);
                }
                index += codeLen;
                continue;
            }

            if (builder != null) {
                builder.append(input.charAt(index));
            }
            index++;
        }

        return builder == null ? input.toString() : builder.toString();
    }

    @FunctionalInterface
    private interface TokenClassifier {
        boolean shouldStrip(CharSequence input, int index, int codeLen);
    }
}
