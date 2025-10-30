package kamkeel.hextext.common.util;

/**
 * Helper utilities for working with sign text limits whilst respecting
 * formatting sequences.
 */
public final class SignTextHelper {

    private SignTextHelper() {
    }

    /**
     * Maximum number of rendered characters permitted on a single sign line.
     */
    public static final int SIGN_LINE_VISIBLE_LIMIT = 15;

    /**
     * Calculates the visible character length of the provided sign line by
     * stripping all recognised formatting codes.
     *
     * @param text sign text to measure
     * @return number of characters that would be rendered
     */
    public static int visibleLength(CharSequence text) {
        if (text == null) {
            return 0;
        }
        return StringUtils.stripColorCodes(text).length();
    }

    /**
     * Ensures the provided sign line does not exceed the visible length limit
     * while preserving formatting codes.
     *
     * @param text sign text to clamp
     * @return a string whose rendered length does not exceed the limit
     */
    public static String clampToVisibleLimit(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        if (visibleLength(text) <= SIGN_LINE_VISIBLE_LIMIT) {
            return text;
        }

        StringBuilder builder = new StringBuilder(text.length());
        int index = 0;
        int visible = 0;

        while (index < text.length()) {
            int codeLength = ColorCodeUtils.detectColorCodeLengthIgnoringRaw(text, index);
            if (codeLength > 0) {
                builder.append(text, index, index + codeLength);
                index += codeLength;
                continue;
            }

            if (visible >= SIGN_LINE_VISIBLE_LIMIT) {
                break;
            }

            builder.append(text.charAt(index));
            visible++;
            index++;
        }

        while (index < text.length()) {
            int codeLength = ColorCodeUtils.detectColorCodeLengthIgnoringRaw(text, index);
            if (codeLength <= 0) {
                break;
            }
            builder.append(text, index, index + codeLength);
            index += codeLength;
        }

        return builder.toString();
    }

    /**
     * Raw copy, no transform. Handles nulls/short arrays.
     */
    public static void copyText(String[] src, String[] dst) {
        if (dst == null) return;
        for (int i = 0; i < 4; i++) {
            String s = (src != null && i < src.length) ? src[i] : "";
            dst[i] = (s == null) ? "" : s;
        }
    }

    /**
     * Copy + clamp to visible limit.
     */
    public static void copyTextClamped(String[] src, String[] dst) {
        if (dst == null) return;
        for (int i = 0; i < 4; i++) {
            String s = (src != null && i < src.length) ? src[i] : "";
            dst[i] = clampToVisibleLimit(s == null ? "" : s);
        }
    }

    /**
     * Server-side: sanitize + clamp.
     */
    public static void copyTextSanitizedClamped(String[] src, String[] dst) {
        if (dst == null) return;
        for (int i = 0; i < 4; i++) {
            String s = (src != null && i < src.length) ? src[i] : "";
            if (s == null) s = "";
            s = net.minecraft.util.ChatAllowedCharacters.filerAllowedCharacters(s);
            dst[i] = clampToVisibleLimit(s);
        }
    }
}
