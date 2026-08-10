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
        assertRampAcrossLines(buildMessage(100), 40);
    }

    @Test
    public void manyShortLinesStillReadAsOneRamp() {
        assertRampAcrossLines(buildMessage(90), 12);
    }

    @Test
    public void twoLinesReadAsOneRamp() {
        assertRampAcrossLines(buildMessage(50), 30);
    }

    private static String buildMessage(int glyphs) {
        StringBuilder text = new StringBuilder("&g&#ff0000&#0000ff");
        for (int i = 0; i < glyphs; i++) {
            text.append((char) ('0' + (i % 10)));
        }
        return text.toString();
    }

    private void assertRampAcrossLines(String message, int glyphsPerLine) {
        List<String> lines = splitLikeChat(message, glyphsPerLine);
        assertTrue("message did not wrap", lines.size() > 1);

        List<Integer> colors = new ArrayList<>();
        for (String line : lines) {
            colors.addAll(renderLine(line));
        }

        int total = colors.size();
        // The largest step one ramp can take between neighbours, with slack for
        // per-line rounding: each line quantises its own span.
        int idealStep = (int) Math.ceil(255.0 / Math.max(1, total - 1));
        int allowedStep = idealStep * 4 + 8;

        int previousBlue = -1;
        int previousRed = 256;
        for (int i = 0; i < total; i++) {
            int red = (colors.get(i) >> 16) & 0xFF;
            int blue = colors.get(i) & 0xFF;

            assertTrue("ramp restarted: red rose from " + previousRed + " to " + red
                + " at glyph " + i + " of " + total, red <= previousRed + allowedStep);
            assertTrue("ramp went backwards: blue fell from " + previousBlue + " to " + blue
                + " at glyph " + i + " of " + total, blue >= previousBlue - allowedStep);
            assertTrue("ramp snapped: blue jumped " + (blue - previousBlue)
                + " at glyph " + i + " of " + total, i == 0 || blue - previousBlue <= allowedStep);

            previousRed = red;
            previousBlue = blue;
        }

        int lastBlue = colors.get(total - 1) & 0xFF;
        assertTrue("ramp never arrived: final blue " + lastBlue, lastBlue >= 0xFF - idealStep * 2);
    }

    /**
     * Vanilla's chat split with the mixin's transforms applied at the same two
     * points: the first line is cut back to the boundary colour, the continuation
     * opens with the carry token.
     */
    private List<String> splitLikeChat(String message, int glyphsPerLine) {
        List<String> lines = new ArrayList<>();
        String s = message;
        for (int guard = 0; guard < 64; guard++) {
            String s1 = trimToGlyphs(s, glyphsPerLine);
            if (s1.length() >= s.length()) {
                lines.add(s);
                return lines;
            }
            String s2 = s.substring(s1.length());

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
                prefix = StringUtils.extractFormatFromString(s1);
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
