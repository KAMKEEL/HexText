package kamkeel.hextext.client;

import kamkeel.hextext.util.ColorCodeUtils;

/**
 * Utility methods for measuring formatted Minecraft text for the legacy renderer.
 */
public final class FormattedTextMetrics {

    private FormattedTextMetrics() {}

    @FunctionalInterface
    public interface CharWidthFunction {
        float getWidth(char character);
    }

    public static float calculateMaxLineWidth(CharSequence text, boolean rawMode,
            CharWidthFunction charWidthFunc, float glyphSpacing, float boldExtra) {
        if (text == null || text.length() == 0) {
            return 0.0f;
        }

        float maxWidth = 0.0f;
        float currentLineWidth = 0.0f;
        boolean isBold = false;
        final int length = text.length();

        for (int index = 0; index < length; ) {
            if (!rawMode) {
                int codeLen = ColorCodeUtils.detectColorCodeLength(text, index);
                if (codeLen > 0) {
                    isBold = updateBoldFlag(text, index, codeLen, isBold);
                    index += codeLen;
                    continue;
                }
            }

            char character = text.charAt(index);
            if (character == '\n') {
                maxWidth = Math.max(maxWidth, currentLineWidth);
                currentLineWidth = 0.0f;
                index++;
                continue;
            }

            float charWidth = charWidthFunc.getWidth(character);
            if (charWidth > 0.0f) {
                currentLineWidth += charWidth;
                if (isBold) {
                    currentLineWidth += boldExtra;
                }
                currentLineWidth += glyphSpacing;
                maxWidth = Math.max(maxWidth, currentLineWidth);
            }
            index++;
        }

        return Math.max(maxWidth, currentLineWidth);
    }

    public static int computeLineBreakIndex(CharSequence text, int maxWidth, boolean rawMode,
            CharWidthFunction charWidthFunc, float glyphSpacing, float boldExtra) {
        if (text == null || text.length() == 0 || maxWidth <= 0) {
            return 0;
        }

        int lastSafePosition = 0;
        float currentWidth = 0.0f;
        boolean isBold = false;
        final int length = text.length();

        for (int index = 0; index < length; ) {
            if (!rawMode) {
                int codeLen = ColorCodeUtils.detectColorCodeLength(text, index);
                if (codeLen > 0) {
                    isBold = updateBoldFlag(text, index, codeLen, isBold);
                    index += codeLen;
                    lastSafePosition = index;
                    continue;
                }
            }

            char character = text.charAt(index);
            if (character == '\n') {
                return index;
            }

            float charWidth = charWidthFunc.getWidth(character);
            if (charWidth < 0.0f) {
                charWidth = 0.0f;
            }

            float nextWidth = currentWidth;
            if (charWidth > 0.0f) {
                nextWidth += charWidth;
                if (isBold) {
                    nextWidth += boldExtra;
                }
                nextWidth += glyphSpacing;
            }

            if (nextWidth > maxWidth) {
                return Math.min(lastSafePosition, length);
            }

            currentWidth = nextWidth;
            index++;
            lastSafePosition = index;
        }

        return length;
    }

    private static boolean updateBoldFlag(CharSequence text, int index, int codeLen, boolean currentBold) {
        if (codeLen == 2 && index + 1 < text.length()) {
            char fmt = Character.toLowerCase(text.charAt(index + 1));
            if (fmt == 'l') {
                return true;
            }
            if (fmt == 'r' || ColorCodeUtils.isMinecraftColorCode(fmt)) {
                return false;
            }
            return currentBold;
        }
        if (codeLen > 2) {
            return false;
        }
        return currentBold;
    }
}
