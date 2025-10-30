package kamkeel.hextext.common.util;

import kamkeel.hextext.config.HexTextConfig;

/**
 * Utility helpers for parsing and working with Minecraft formatting and RGB colour codes.
 */
public final class ColorCodeUtils {

    private ColorCodeUtils() {}

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

    public static boolean isFormattingCode(char c) {
        char lower = Character.toLowerCase(c);
        return isMinecraftColorCode(lower)
            || lower == 'g'
            || lower == 'h'
            || lower == 'i'
            || lower == 'j'
            || isStyleCode(lower)
            || isResetCode(lower);
    }

    public static boolean isMinecraftColorCode(char c) {
        char lower = Character.toLowerCase(c);
        return (lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f');
    }

    public static int getMinecraftColorIndex(char c) {
        char lower = Character.toLowerCase(c);
        if (lower >= '0' && lower <= '9') {
            return lower - '0';
        }
        if (lower >= 'a' && lower <= 'f') {
            return 10 + (lower - 'a');
        }
        return -1;
    }

    public static boolean isStyleCode(char c) {
        char lower = Character.toLowerCase(c);
        return lower == 'k' || lower == 'l' || lower == 'm' || lower == 'n' || lower == 'o';
    }

    public static boolean isEffectCode(char c) {
        char lower = Character.toLowerCase(c);
        return lower == 'g' || lower == 'h' || lower == 'i' || lower == 'j';
    }

    public static boolean isResetCode(char c) {
        return Character.toLowerCase(c) == 'r';
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

    public static int detectColorCodeLength(CharSequence str, int pos, boolean rawMode) {
        if (str == null || pos < 0 || pos >= str.length() || rawMode) {
            return 0;
        }

        char c = str.charAt(pos);
        boolean allowAmpersand = HexTextConfig.isAmpersandAllowed();
        boolean allowHtml = HexTextConfig.isRgbHtmlFormatEnabled();

        if (c == 167) {
            if (pos + 2 <= str.length() && str.charAt(pos + 1) == '#'
                && pos + 8 <= str.length() && isValidHexString(str, pos + 2)) {
                return 8;
            }
            if (pos + 1 < str.length() && isFormattingCode(str.charAt(pos + 1))) {
                return 2;
            }
            return 0;
        }

        if (c == '&') {
            if (!allowAmpersand) {
                return 0;
            }
            if (pos + 2 <= str.length() && str.charAt(pos + 1) == '#'
                && pos + 8 <= str.length() && isValidHexString(str, pos + 2)) {
                return 8;
            }
            if (pos + 1 < str.length() && isFormattingCode(str.charAt(pos + 1))) {
                return 2;
            }
            return 0;
        }

        if (allowHtml && c == '<' && pos + 9 <= str.length() && str.charAt(pos + 1) == '/'
            && str.charAt(pos + 8) == '>' && isValidHexString(str, pos + 2)) {
            return 9;
        }

        if (allowHtml && c == '<' && pos + 8 <= str.length() && str.charAt(pos + 7) == '>'
            && isValidHexString(str, pos + 1)) {
            return 8;
        }

        return 0;
    }

    public static int detectColorCodeLength(CharSequence str, int pos) {
        return detectColorCodeLength(str, pos, false);
    }

    public static int detectColorCodeLengthIgnoringRaw(CharSequence str, int pos) {
        return detectColorCodeLength(str, pos, false);
    }

    public static int calculateShadowColor(int rgb) {
        return (rgb & 0xFCFCFC) >> 2;
    }

    public static int hsvToRgb(float hue, float saturation, float value) {
        hue = hue % 360.0f;
        if (hue < 0) {
            hue += 360.0f;
        }

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

        float r;
        float g;
        float b;
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

    /**
     * Checks if the supplied character sequence contains any recognised Minecraft or HexText
     * formatting tokens.
     *
     * @param input sequence to inspect
     * @return {@code true} when a formatting token is encountered, {@code false} otherwise
     */
    public static boolean containsFormattingCodes(CharSequence input) {
        if (input == null || input.length() == 0) {
            return false;
        }

        for (int index = 0; index < input.length(); index++) {
            if (detectColorCodeLengthIgnoringRaw(input, index) > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds the index of the first formatting token at or after {@code start}.
     *
     * @param input sequence to inspect
     * @param start index to begin searching from
     * @return the index of the first token or {@code -1} when none exist in the range
     */
    public static int indexOfNextFormattingCode(CharSequence input, int start) {
        if (input == null || start < 0 || start >= input.length()) {
            return -1;
        }

        for (int index = start; index < input.length(); index++) {
            if (detectColorCodeLengthIgnoringRaw(input, index) > 0) {
                return index;
            }
        }

        return -1;
    }
}
