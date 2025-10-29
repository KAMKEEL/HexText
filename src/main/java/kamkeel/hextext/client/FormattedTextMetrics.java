package kamkeel.hextext.client;

import kamkeel.hextext.util.ColorCodeUtils;

/**
 * Utility methods for measuring formatted Minecraft text for the legacy renderer.
 */
public final class FormattedTextMetrics {

    private FormattedTextMetrics() {}

    public static float calculateMaxLineWidth(CharSequence text, boolean rawMode,
            CharWidthFunction charWidthFunc, float glyphSpacing, float boldExtra) {
        if (text == null || text.length() == 0) {
            return 0.0f;
        }

        float maxWidth = 0.0f;
        float currentLineWidth = 0.0f;
        LegacyFormattingState state = new LegacyFormattingState();
        final int length = text.length();

        for (int index = 0; index < length; index++) {
            if (!rawMode) {
                int consumed = consumeFormatting(text, length, index, state);
                if (consumed > 0) {
                    index += consumed - 1;
                    continue;
                }
            }

            char character = text.charAt(index);
            if (character == '\n') {
                maxWidth = Math.max(maxWidth, currentLineWidth);
                currentLineWidth = 0.0f;
                state.setExpectingLegacyCode(false);
                continue;
            }

            float charWidth = charWidthFunc.getWidth(character);
            if (charWidth > 0.0f) {
                currentLineWidth += charWidth;
                if (state.isBold()) {
                    currentLineWidth += boldExtra;
                }
                currentLineWidth += glyphSpacing;
                maxWidth = Math.max(maxWidth, currentLineWidth);
            }
        }

        return Math.max(maxWidth, currentLineWidth);
    }

    public static int computeLineBreakIndex(CharSequence text, int maxWidth, boolean rawMode,
            CharWidthFunction charWidthFunc, float glyphSpacing, float boldExtra) {
        if (text == null || text.length() == 0 || maxWidth <= 0) {
            return 0;
        }

        int lastSpaceIndex = -1;
        float currentWidth = 0.0f;
        LegacyFormattingState state = new LegacyFormattingState();
        final int length = text.length();

        for (int index = 0; index < length; index++) {
            if (!rawMode) {
                int consumed = consumeFormatting(text, length, index, state);
                if (consumed > 0) {
                    index += consumed - 1;
                    continue;
                }
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
                if (state.isBold()) {
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
        }

        return length;
    }

    private static int consumeFormatting(CharSequence text, int length, int index, LegacyFormattingState state) {
        if (index >= length) {
            return 0;
        }

        if (state.isExpectingLegacyCode()) {
            state.setExpectingLegacyCode(false);
            applyLegacyCode(text.charAt(index), state);
            return 1;
        }

        char current = text.charAt(index);

        if (current == 167 || current == '&') {
            if (current == '&' && index + 7 <= length && ColorCodeUtils.isValidHexString(text, index + 1)) {
                state.setBold(false);
                return 7;
            }

            if (current == '&' && (index + 1 >= length || !ColorCodeUtils.isFormattingCode(text.charAt(index + 1)))) {
                return 0;
            }

            state.setExpectingLegacyCode(true);
            return 1;
        }

        if (current == '<') {
            if (index + 8 <= length && text.charAt(index + 7) == '>' && ColorCodeUtils.isValidHexString(text, index + 1)) {
                state.setBold(false);
                return 8;
            }

            if (index + 9 <= length && text.charAt(index + 1) == '/' && text.charAt(index + 8) == '>'
                && ColorCodeUtils.isValidHexString(text, index + 2)) {
                state.setBold(false);
                return 9;
            }
        }

        return 0;
    }

    private static void applyLegacyCode(char formatChar, LegacyFormattingState state) {
        char fmt = Character.toLowerCase(formatChar);
        if (fmt == 'l') {
            state.setBold(true);
            return;
        }

        if (fmt == 'r' || ColorCodeUtils.isMinecraftColorCode(fmt) || fmt == 'g') {
            state.setBold(false);
        }
    }
}
