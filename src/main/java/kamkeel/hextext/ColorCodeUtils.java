package kamkeel.hextext;

import kamkeel.hextext.client.LegacyFontRenderContext;

import java.util.ArrayDeque;

/**
 * Utility class for parsing RGB color codes in text for the legacy font renderer.
 */
public class ColorCodeUtils {

    public static boolean isValidHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    public static boolean isValidHexString(String hex) {
        if (hex == null || hex.length() != 6) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            if (!isValidHexChar(hex.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isFormattingCode(char c) {
        char lower = Character.toLowerCase(c);
        return (lower >= '0' && lower <= '9')
            || (lower >= 'a' && lower <= 'f')
            || lower == 'g'
            || lower == 'h'
            || (lower >= 'k' && lower <= 'o')
            || lower == 'r';
    }

    public static boolean isValidHexString(CharSequence str, int start) {
        if (str == null || start < 0 || start + 6 > str.length()) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            if (!isValidHexChar(str.charAt(start + i))) {
                return false;
            }
        }
        return true;
    }

    public static int parseHexColor(String hex) {
        if (!isValidHexString(hex)) {
            return -1;
        }
        try {
            return Integer.parseInt(hex, 16) & 0x00FFFFFF;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static int parseHexColor(CharSequence str, int start) {
        if (!isValidHexString(str, start)) {
            return -1;
        }
        try {
            String hex = str.subSequence(start, start + 6).toString();
            return Integer.parseInt(hex, 16) & 0x00FFFFFF;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return -1;
        }
    }

    public static int detectColorCodeLength(CharSequence str, int pos) {
        return detectColorCodeLengthInternal(str, pos, LegacyFontRenderContext.isRawTextRendering());
    }

    public static int detectColorCodeLengthIgnoringRaw(CharSequence str, int pos) {
        return detectColorCodeLengthInternal(str, pos, false);
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

    private static int detectColorCodeLengthInternal(CharSequence str, int pos, boolean skipDueToRaw) {
        if (str == null || pos < 0 || pos >= str.length()) {
            return 0;
        }

        if (skipDueToRaw) {
            return 0;
        }

        char c = str.charAt(pos);

        if (c == 167 && pos + 1 < str.length()) {
            return 2;
        }

        if (c == '&' && pos + 7 <= str.length()) {
            if (isValidHexString(str, pos + 1)) {
                return 7;
            }
        }

        if (c == '&' && pos + 1 < str.length() && isFormattingCode(str.charAt(pos + 1))) {
            return 2;
        }

        if (c == '<' && pos + 9 <= str.length() && str.charAt(pos + 1) == '/' && str.charAt(pos + 8) == '>') {
            if (isValidHexString(str, pos + 2)) {
                return 9;
            }
        }

        if (c == '<' && pos + 8 <= str.length() && str.charAt(pos + 7) == '>') {
            if (isValidHexString(str, pos + 1)) {
                return 8;
            }
        }

        return 0;
    }

    public static int calculateShadowColor(int rgb) {
        return (rgb & 0xFCFCFC) >> 2;
    }

    public static int hsvToRgb(float hue, float saturation, float value) {
        hue = hue % 360.0f;
        if (hue < 0) hue += 360.0f;

        if (saturation == 0) {
            int gray = (int) (value * 255);
            return (gray << 16) | (gray << 8) | gray;
        }

        float h = hue / 60.0f;
        int sector = (int) Math.floor(h);
        float fractionalSector = h - sector;

        float p = value * (1.0f - saturation);
        float q = value * (1.0f - saturation * fractionalSector);
        float t = value * (1.0f - saturation * (1.0f - fractionalSector));

        float r, g, b;
        switch (sector) {
            case 0:  r = value; g = t;     b = p;     break;
            case 1:  r = q;     g = value; b = p;     break;
            case 2:  r = p;     g = value; b = t;     break;
            case 3:  r = p;     g = q;     b = value; break;
            case 4:  r = t;     g = p;     b = value; break;
            default: r = value; g = p;     b = q;     break;
        }

        int red = (int) (r * 255);
        int green = (int) (g * 255);
        int blue = (int) (b * 255);

        return (red << 16) | (green << 8) | blue;
    }

    public static String extractFormatFromString(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        String currentColorCode = null;
        StringBuilder styleCodes = new StringBuilder();
        ArrayDeque<String> colorStack = new ArrayDeque<>();

        for (int i = 0; i < str.length(); ) {
            int codeLen = detectColorCodeLengthIgnoringRaw(str, i);

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

                    if ((fmt >= '0' && fmt <= '9') || (fmt >= 'a' && fmt <= 'f')) {
                        currentColorCode = code;
                        colorStack.clear();
                        styleCodes.setLength(0);
                    } else if (fmt == 'r') {
                        currentColorCode = null;
                        colorStack.clear();
                        styleCodes.setLength(0);
                    } else if (fmt == 'l' || fmt == 'o' || fmt == 'n' || fmt == 'm' || fmt == 'k') {
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
            int codeLen = detectColorCodeLengthIgnoringRaw(input, index);
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
