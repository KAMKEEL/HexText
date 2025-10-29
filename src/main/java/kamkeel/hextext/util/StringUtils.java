package kamkeel.hextext.util;

import java.util.ArrayDeque;

/**
 * String-related helpers for dealing with Minecraft formatting codes.
 */
public final class StringUtils {

    private StringUtils() {}

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

    public static String extractFormatFromString(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        String currentColorCode = null;
        StringBuilder formatCodes = new StringBuilder();
        ArrayDeque<String> colorStack = new ArrayDeque<>();

        for (int i = 0; i < str.length(); ) {
            int codeLen = ColorCodeUtils.detectColorCodeLengthIgnoringRaw(str, i);

            if (codeLen > 0) {
                char firstChar = str.charAt(i);
                String code = str.substring(i, i + codeLen);

                if (codeLen == 7 && firstChar == '&') {
                    currentColorCode = code;
                    colorStack.clear();
                    formatCodes.setLength(0);
                } else if (codeLen == 8 && firstChar == '<') {
                    if (currentColorCode != null) {
                        colorStack.push(currentColorCode);
                    }
                    currentColorCode = code;
                    formatCodes.setLength(0);
                } else if (codeLen == 9 && firstChar == '<') {
                    currentColorCode = colorStack.isEmpty() ? null : colorStack.pop();
                    formatCodes.setLength(0);
                } else if (codeLen == 2) {
                    char fmt = Character.toLowerCase(str.charAt(i + 1));

                    if (ColorCodeUtils.isMinecraftColorCode(fmt) || fmt == 'g') {
                        currentColorCode = code;
                        colorStack.clear();
                        formatCodes.setLength(0);
                    } else if (ColorCodeUtils.isResetCode(fmt)) {
                        currentColorCode = null;
                        colorStack.clear();
                        formatCodes.setLength(0);
                    } else if (ColorCodeUtils.isStyleCode(fmt) || (ColorCodeUtils.isEffectCode(fmt) && fmt != 'g')) {
                        formatCodes.append(code);
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
        if (formatCodes.length() > 0) {
            result.append(formatCodes);
        }

        return result.toString();
    }

    public static String stripColorCodes(CharSequence input) {
        if (input == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); ) {
            int codeLen = ColorCodeUtils.detectColorCodeLengthIgnoringRaw(input, index);
            if (codeLen > 0) {
                index += codeLen;
                continue;
            }

            builder.append(input.charAt(index));
            index++;
        }

        return builder.toString();
    }
}
