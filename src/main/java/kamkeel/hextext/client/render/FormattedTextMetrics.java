package kamkeel.hextext.client.render;

import kamkeel.hextext.common.util.ColorCodeUtils;

/**
 * Utility methods for measuring formatted Minecraft text for the legacy renderer.
 */
public final class FormattedTextMetrics {

    private FormattedTextMetrics() {
    }

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

        ColorCodeUtils.FormattingEnvironment env = rawMode ? null : ColorCodeUtils.captureFormattingEnvironment(false);
        // Angelica carries styles through a colour where HexText clears them, and the
        // widths have to follow whichever renderer is drawing. Read once per string.
        final boolean hexResetsStyles = ColorCodeUtils.hexResetsStyles();

        for (int index = 0; index < length; ) {
            if (!rawMode) {
                int codeLen = ColorCodeUtils.detectColorCodeLength(text, index, false, env);
                if (codeLen > 0) {
                    if (codeLen == 2 && index + 1 < length) {
                        char fmt = Character.toLowerCase(text.charAt(index + 1));
                        if (fmt == 'l') {
                            isBold = true;
                        } else if (fmt == 'r') {
                            isBold = false;
                        } else if ((fmt >= '0' && fmt <= '9') || (fmt >= 'a' && fmt <= 'f')) {
                            isBold = false;
                        }
                    }
                    index += codeLen;
                    continue;
                }
            } else {
                // RAW MODE: the codes are drawn as characters, and a code applies to its
                // own two characters - both renderers do this, so widths must too.
                if (index + 1 < length) {
                    char marker = text.charAt(index);
                    if (marker == 167 || marker == '&') { // '§' or '&'
                        char fmt = Character.toLowerCase(text.charAt(index + 1));
                        if (fmt == '#') {
                            isBold &= !hexResetsStyles;
                        } else if (ColorCodeUtils.isFormattingCode(fmt)) {
                            if (fmt == 'l') {
                                isBold = true;
                            } else if (fmt == 'r' || ColorCodeUtils.isMinecraftColorCode(fmt)) {
                                isBold = false;
                            }
                        }
                    } else if (marker == '<') {
                        isBold &= !hexResetsStyles;
                    }
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
        return computeBreakIndex(text, maxWidth, rawMode, charWidthFunc, glyphSpacing, boldExtra, true);
    }

    public static int computeTrimIndex(CharSequence text, int maxWidth, boolean rawMode,
                                       CharWidthFunction charWidthFunc, float glyphSpacing, float boldExtra) {
        return computeBreakIndex(text, maxWidth, rawMode, charWidthFunc, glyphSpacing, boldExtra, false);
    }

    private static int computeBreakIndex(CharSequence text, int maxWidth, boolean rawMode,
                                          CharWidthFunction charWidthFunc, float glyphSpacing, float boldExtra,
                                          boolean preferSpaceBreak) {
        if (text == null || text.length() == 0 || maxWidth <= 0) {
            return 0;
        }

        int lastSafePosition = 0;
        int lastSpacePosition = -1;    // **index of last space character**
        float currentWidth = 0.0f;
        boolean isBold = false;
        final int length = text.length();

        ColorCodeUtils.FormattingEnvironment env = rawMode ? null : ColorCodeUtils.captureFormattingEnvironment(false);
        // Angelica carries styles through a colour where HexText clears them, and the
        // widths have to follow whichever renderer is drawing. Read once per string.
        final boolean hexResetsStyles = ColorCodeUtils.hexResetsStyles();

        for (int index = 0; index < length; ) {
            if (!rawMode) {
                int codeLen = ColorCodeUtils.detectColorCodeLength(text, index, false, env);
                if (codeLen > 0) {
                    if (codeLen == 2 && index + 1 < length) {
                        char fmt = Character.toLowerCase(text.charAt(index + 1));
                        if (fmt == 'l') {
                            isBold = true;
                        } else if (fmt == 'r') {
                            isBold = false;
                        } else if ((fmt >= '0' && fmt <= '9') || (fmt >= 'a' && fmt <= 'f')) {
                            isBold = false;
                        }
                    }
                    index += codeLen;
                    lastSafePosition = index;
                    continue;
                }
            } else {
                // RAW MODE: respect style/color codes for measuring, but don't strip them.
                // As above, a code applies to its own two characters.
                if (index + 1 < length) {
                    char marker = text.charAt(index);
                    if (marker == 167 || marker == '&') {
                        char fmt = Character.toLowerCase(text.charAt(index + 1));
                        if (fmt == '#') {
                            isBold = false;
                        } else if (ColorCodeUtils.isFormattingCode(fmt)) {
                            if (fmt == 'l') {
                                isBold = true;
                            } else if (fmt == 'r' || ColorCodeUtils.isMinecraftColorCode(fmt)) {
                                isBold = false;
                            }
                        }
                    } else if (marker == '<') {
                        isBold &= !hexResetsStyles;
                    }
                }
            }

            char character = text.charAt(index);
            if (character == '\n') {
                return index;
            }

            if (preferSpaceBreak && character == ' ') {
                lastSpacePosition = index;
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
                // Prefer breaking at the last space, otherwise fall back to lastSafePosition
                if (preferSpaceBreak && lastSpacePosition >= 0) {
                    return lastSpacePosition; // your wrapper will see ' ' at breakPoint and skip it
                }
                return Math.min(lastSafePosition, length);
            }
            currentWidth = nextWidth;
            index++;
            lastSafePosition = index;
        }

        return length;
    }
}
