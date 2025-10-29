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

        for (int index = 0; index < length; ) {
            if (!rawMode) {
                int consumed = consumeFormatting(text, length, index, state);
                if (consumed != index) {
                    index = consumed;
                    continue;
                }
            }

            char character = text.charAt(index);
            if (character == '\n') {
                maxWidth = Math.max(maxWidth, currentLineWidth);
                currentLineWidth = 0.0f;
                state.setExpectingLegacyCode(false);
                index++;
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
            index++;
        }

        return Math.max(maxWidth, currentLineWidth);
    }

    public static int computeLineBreakIndex(CharSequence text, int maxWidth, boolean rawMode,
            CharWidthFunction charWidthFunc, float glyphSpacing, float boldExtra) {
        if (text == null || text.length() == 0 || maxWidth <= 0) {
            return 0;
        }

        int lastSpaceIndex = -1;
        int lastSafeIndex = 0;
        float currentWidth = 0.0f;
        LegacyFormattingState state = new LegacyFormattingState();
        final int length = text.length();

        for (int index = 0; index < length; ) {
            if (!rawMode) {
                int consumed = consumeFormatting(text, length, index, state);
                if (consumed != index) {
                    index = consumed;
                    lastSafeIndex = index;
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
                return Math.max(lastSafeIndex, index);
            }

            currentWidth = nextWidth;
            index++;
            lastSafeIndex = index;
        }

        return length;
    }

    private static int consumeFormatting(CharSequence text, int length, int index, LegacyFormattingState state) {
        if (index >= length) {
            return index;
        }

        if (state.isExpectingLegacyCode()) {
            state.setExpectingLegacyCode(false);
            applyLegacyCode(text.charAt(index), state);
            return index + 1;
        }

        char current = text.charAt(index);

        if (current == 167 || current == '&') {
            if (current == '&' && index + 7 <= length && ColorCodeUtils.isValidHexString(text, index + 1)) {
                state.setBold(false);
                return index + 7;
            }

            if (current == '&' && (index + 1 >= length || !ColorCodeUtils.isFormattingCode(text.charAt(index + 1)))) {
                return index;
            }

            state.setExpectingLegacyCode(true);
            return index + 1;
        }

        if (current == '<') {
            if (index + 8 <= length && text.charAt(index + 7) == '>' && ColorCodeUtils.isValidHexString(text, index + 1)) {
                state.setBold(false);
                return index + 8;
            }

            if (index + 9 <= length && text.charAt(index + 1) == '/' && text.charAt(index + 8) == '>'
                && ColorCodeUtils.isValidHexString(text, index + 2)) {
                state.setBold(false);
                return index + 9;
            }
        }

        return index;
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
