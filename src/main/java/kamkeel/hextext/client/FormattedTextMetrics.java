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

        FormattingTracker tracker = new FormattingTracker();
        float maxWidth = 0.0f;
        float currentLineWidth = 0.0f;
        final int length = text.length();

        for (int index = 0; index < length; ) {
            int consumed = tracker.consumeFormatting(text, index, rawMode);
            if (consumed > 0) {
                index += consumed;
                continue;
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
                if (tracker.isBold) {
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

        FormattingTracker tracker = new FormattingTracker();
        int lastSpaceIndex = -1;
        float currentWidth = 0.0f;
        final int length = text.length();

        for (int index = 0; index < length; ) {
            int consumed = tracker.consumeFormatting(text, index, rawMode);
            if (consumed > 0) {
                index += consumed;
                continue;
            }

            char character = text.charAt(index);
            if (character == '\n') {
                return index;
            }

            if (character == ' ') {
                lastSpaceIndex = index;
            }

            float charWidth = charWidthFunc.getWidth(character);
            if (charWidth < 0.0f) {
                charWidth = 0.0f;
            }

            float nextWidth = currentWidth;
            if (charWidth > 0.0f) {
                nextWidth += charWidth;
                if (tracker.isBold) {
                    nextWidth += boldExtra;
                }
                nextWidth += glyphSpacing;
            }

            if (nextWidth > maxWidth) {
                if (lastSpaceIndex != -1 && lastSpaceIndex < index) {
                    return lastSpaceIndex;
                }
                return index;
            }

            currentWidth = nextWidth;
            index++;
        }

        return length;
    }

    private static final class FormattingTracker {

        boolean isBold;

        int consumeFormatting(CharSequence text, int index, boolean rawMode) {
            if (rawMode || index < 0 || index >= text.length()) {
                return 0;
            }

            char first = text.charAt(index);
            if (first == 167) {
                if (index + 1 >= text.length()) {
                    return 0;
                }
                char fmt = Character.toLowerCase(text.charAt(index + 1));
                if (fmt == 'l') {
                    isBold = true;
                } else if (fmt == 'r' || ColorCodeUtils.isMinecraftColorCode(fmt) || fmt == 'g') {
                    isBold = false;
                }
                return 2;
            }

            if (first == '&') {
                if (index + 7 <= text.length() && ColorCodeUtils.isValidHexString(text, index + 1)) {
                    isBold = false;
                    return 7;
                }

                if (index + 1 < text.length()) {
                    char fmt = Character.toLowerCase(text.charAt(index + 1));
                    if (ColorCodeUtils.isFormattingCode(fmt)) {
                        if (fmt == 'l') {
                            isBold = true;
                        } else if (ColorCodeUtils.isResetCode(fmt)
                            || ColorCodeUtils.isMinecraftColorCode(fmt)
                            || fmt == 'g') {
                            isBold = false;
                        }
                        return 2;
                    }
                }
            }

            if (first == '<') {
                if (index + 8 <= text.length() && text.charAt(index + 7) == '>'
                    && ColorCodeUtils.isValidHexString(text, index + 1)) {
                    isBold = false;
                    return 8;
                }

                if (index + 9 <= text.length() && text.charAt(index + 1) == '/'
                    && text.charAt(index + 8) == '>' && ColorCodeUtils.isValidHexString(text, index + 2)) {
                    isBold = false;
                    return 9;
                }
            }

            return 0;
        }
    }
}
