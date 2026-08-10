package kamkeel.hextext.common.util;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.config.HexTextConfig;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Replays the chat pipeline end to end on paper: vanilla's split loop, the mixin's
 * carry, and the renderer's per-glyph colour, then checks the glyph colours read
 * as one ramp. A restarted ramp shows up as red rising again; a snapped one as a
 * step bigger than the ramp's own slope allows.
 */
public class GradientChatWrapSimulationTest {

    private static final int START = 0xFF0000;
    private static final int END = 0x0000FF;

    @Before
    public void setUp() {
        HexText.proxy = new CommonProxy();
        HexTextConfig.resetToDefaults();
        HexTextConfig.setUniversalAmpersandEnabled(true);
        HexTextConfig.setEnableRgbHtmlFormat(true);
    }

    @Test
    public void wrappedChatMessageReadsAsOneRamp() {
        assertRampAcrossLines(buildMessage(100), 40, false);
    }

    @Test
    public void manyShortLinesStillReadAsOneRamp() {
        assertRampAcrossLines(buildMessage(90), 12, false);
    }

    @Test
    public void twoLinesReadAsOneRamp() {
        assertRampAcrossLines(buildMessage(50), 30, false);
    }

    /**
     * Hodgepodge prepends the first line's extracted format to every continuation
     * before this mod's handler runs - which, with a gradient-aware extractor, is
     * the whole original token. Unpeeled it sat after the carry token and restarted
     * the ramp on every wrapped line.
     */
    @Test
    public void hodgepodgePrependDoesNotRestartTheRamp() {
        assertRampAcrossLines(buildMessage(100), 40, true);
        assertRampAcrossLines(buildMessage(90), 12, true);
    }

    private static String buildMessage(int glyphs) {
        StringBuilder text = new StringBuilder("&g&#ff0000&#0000ff");
        for (int i = 0; i < glyphs; i++) {
            text.append((char) ('0' + (i % 10)));
        }
        return text.toString();
    }

    private void assertRampAcrossLines(String message, int glyphsPerLine, boolean hodgepodgePrepend) {
        List<String> lines = splitLikeChat(message, glyphsPerLine, hodgepodgePrepend);
        assertTrue("message did not wrap", lines.size() > 1);

        List<Integer> colors = new ArrayList<>();
        for (String line : lines) {
            colors.addAll(renderLine(line));
        }

        // Every glyph is held against the colour an unwrapped message would give
        // it. This catches a restart, a snap and a drift alike, and is agnostic to
        // the colour space the ramp is walked in. The tolerance covers each line
        // quantising its own span.
        int total = colors.size();
        for (int i = 0; i < total; i++) {
            int expected = TextEffectMath.computeGradientColor(START, END, i, total);
            int actual = colors.get(i);
            for (int shift = 0; shift <= 16; shift += 8) {
                int expectedChannel = (expected >> shift) & 0xFF;
                int actualChannel = (actual >> shift) & 0xFF;
                assertTrue("glyph " + i + " of " + total + " strayed from the ramp: expected "
                        + String.format("%06x", expected) + " but drew " + String.format("%06x", actual),
                    Math.abs(expectedChannel - actualChannel) <= 24);
            }
        }
    }

    /**
     * Vanilla's chat split with the mixin's transforms applied at the same two
     * points: the first line is cut back to the boundary colour, the continuation
     * opens with the carry token.
     */
    private List<String> splitLikeChat(String message, int glyphsPerLine, boolean hodgepodgePrepend) {
        List<String> lines = new ArrayList<>();
        String s = message;
        for (int guard = 0; guard < 64; guard++) {
            String s1 = trimToGlyphs(s, glyphsPerLine);
            if (s1.length() >= s.length()) {
                lines.add(s);
                return lines;
            }
            String s2 = s.substring(s1.length());

            if (hodgepodgePrepend) {
                // Hodgepodge's fix runs first and prepends getFormatFromString(s1),
                // which this mod's inject answers with the extractor.
                s2 = StringUtils.extractFormatFromString(s1) + s2;
            }

            // The mixin peels off exactly that duplicate before carrying.
            String duplicatePrefix = StringUtils.extractFormatFromString(s1);
            if (!duplicatePrefix.isEmpty() && s2.startsWith(duplicatePrefix)) {
                s2 = s2.substring(duplicatePrefix.length());
            }

            String prefix;
            GradientWrap.Carry carry = GradientWrap.carryAcrossBreak(s1, s2);
            if (carry != null) {
                s1 = carry.rewrittenFirstPart;
                String styles = StringUtils.extractFormatFromString(carry.rewrittenFirstPart);
                if (styles.startsWith(carry.rewrittenToken)) {
                    styles = styles.substring(carry.rewrittenToken.length());
                }
                prefix = carry.continuationToken + styles;
            } else {
                prefix = duplicatePrefix;
            }

            lines.add(s1);
            s = prefix + s2;
        }
        throw new IllegalStateException("split did not terminate");
    }

    /** Codes are zero-width, glyphs count toward the line budget - like the trim. */
    private static String trimToGlyphs(String text, int glyphs) {
        int seen = 0;
        int i = 0;
        while (i < text.length()) {
            int code = ColorCodeUtils.detectColorCodeLength(text, i);
            if (code > 0) {
                i += code;
                continue;
            }
            if (seen == glyphs) {
                return text.substring(0, i);
            }
            seen++;
            i++;
        }
        return text;
    }

    /** The renderer's view of one line: gradient span counted, colour per glyph. */
    private static List<Integer> renderLine(String line) {
        List<Integer> colors = new ArrayList<>();
        int gradientStart = -1;
        int startRgb = 0;
        int endRgb = 0;
        int span = 0;
        int glyphIndex = 0;

        for (int i = 0; i < line.length(); ) {
            int token = ColorCodeUtils.gradientTokenLength(line, i);
            if (token > 0) {
                startRgb = ColorCodeUtils.parseHexColor(line, i + 4);
                endRgb = ColorCodeUtils.parseHexColor(line, i + 12);
                gradientStart = i + token;
                glyphIndex = 0;
                span = 0;
                for (int j = gradientStart; j < line.length(); ) {
                    int code = ColorCodeUtils.detectColorCodeLength(line, j);
                    if (code > 0) {
                        j += code;
                        continue;
                    }
                    span++;
                    j++;
                }
                i += token;
                continue;
            }
            int code = ColorCodeUtils.detectColorCodeLength(line, i);
            if (code > 0) {
                i += code;
                continue;
            }
            if (gradientStart != -1) {
                colors.add(TextEffectMath.computeGradientColor(startRgb, endRgb, glyphIndex, span));
                glyphIndex++;
            }
            i++;
        }
        return colors;
    }
}
