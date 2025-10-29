package kamkeel.hextext.common.util;

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
        StringBuilder styleCodes = new StringBuilder();
        ArrayDeque<String> colorStack = new ArrayDeque<>();

        for (int i = 0; i < str.length(); ) {
            int codeLen = ColorCodeUtils.detectColorCodeLengthIgnoringRaw(str, i);

            if (codeLen > 0) {
                char firstChar = str.charAt(i);
                String code = str.substring(i, i + codeLen);

                if (codeLen == 7 && firstChar == '&') {
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

    public static boolean containsColorCodes(CharSequence input) {
        if (input == null || input.length() == 0) {
            return false;
        }

        for (int index = 0; index < input.length(); ) {
            int codeLen = ColorCodeUtils.detectColorCodeLengthIgnoringRaw(input, index);
            if (codeLen > 0) {
                return true;
            }

            char current = input.charAt(index);
            if (current == 167) {
                if (index + 1 < input.length()) {
                    char next = input.charAt(index + 1);
                    if (ColorCodeUtils.isFormattingCode(next)) {
                        return true;
                    }
                }
                return true;
            }

            index++;
        }

        return false;
    }

    public static String stripExtras(CharSequence input) {
        String stripped = stripColorCodes(input);
        if (stripped == null || stripped.indexOf(167) < 0) {
            return stripped;
        }

        return stripped.replace(String.valueOf((char) 167), "");
    }
}
