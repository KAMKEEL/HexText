package kamkeel.hextext.common.util;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.config.HexTextConfig;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A gradient split by a line break has to keep drawing one ramp: the first line
 * stops at the colour the ramp has reached, the continuation starts from it, and
 * both ends stay where the author put them.
 */
public class GradientWrapTest {

    @Before
    public void setUp() {
        HexText.proxy = new CommonProxy();
        HexTextConfig.resetToDefaults();
        HexTextConfig.setUniversalAmpersandEnabled(true);
        HexTextConfig.setEnableRgbHtmlFormat(true);
    }

    @Test
    public void splitsTheRampAtTheBreak() {
        // Five glyphs on the first line, five on the second: ten in all.
        GradientWrap.Carry carry = GradientWrap.carryAcrossBreak("&g&#ff0000&#0000ffabcde", "fghij");

        assertTrue(carry != null);
        int boundary = TextEffectMath.computeGradientColor(0xFF0000, 0x0000FF, 5, 10);
        String boundaryHex = String.format("%06x", boundary);

        assertEquals("&g&#ff0000&#" + boundaryHex + "abcde", carry.rewrittenFirstPart);
        assertEquals("&g&#" + boundaryHex + "&#0000ff", carry.continuationToken);
    }

    /** The boundary colour is the renderer's own glyph colour, so the seam is invisible. */
    @Test
    public void boundaryMatchesWhatTheRendererWouldDraw() {
        GradientWrap.Carry carry = GradientWrap.carryAcrossBreak("&g&#ff0000&#0000ffab", "cd");

        int boundary = TextEffectMath.computeGradientColor(0xFF0000, 0x0000FF, 2, 4);
        assertEquals(String.format("&g&#%06x&#0000ff", boundary), carry.continuationToken);
    }

    /** Codes between the glyphs are zero-width to the ramp, exactly as the renderer counts. */
    @Test
    public void formattingCodesAreNotCountedAsDistance() {
        GradientWrap.Carry stylesInside = GradientWrap.carryAcrossBreak("&g&#ff0000&#0000ffab&lcd", "ef");
        GradientWrap.Carry plain = GradientWrap.carryAcrossBreak("&g&#ff0000&#0000ffabcd", "ef");

        assertEquals(plain.continuationToken, stylesInside.continuationToken);
    }

    @Test
    public void sectionSpellingIsPreserved() {
        GradientWrap.Carry carry = GradientWrap.carryAcrossBreak("§g§#ff0000§#0000ffabcde", "fghij");

        assertTrue(carry.continuationToken.startsWith("§g§#"));
        assertTrue(carry.continuationToken.endsWith("§#0000ff".substring(1)));
    }

    @Test
    public void terminatedGradientIsNotCarried() {
        assertNull(GradientWrap.carryAcrossBreak("&g&#ff0000&#0000ffab &ccd", "ef"));
        assertNull(GradientWrap.carryAcrossBreak("&g&#ff0000&#0000ffab &rcd", "ef"));
        assertNull(GradientWrap.carryAcrossBreak("&g&#ff0000&#0000ffab &#00ff00cd", "ef"));
    }

    /** A second gradient takes over; the carry belongs to it, colours and all. */
    @Test
    public void laterGradientWinsTheCarry() {
        GradientWrap.Carry carry = GradientWrap.carryAcrossBreak(
            "&g&#ff0000&#0000ffab &g&#00ff00&#ffffffcd", "ef");

        assertTrue(carry.continuationToken.endsWith("&#ffffff"));
    }

    /** The rest is only counted up to where the ramp ends, not past its terminator. */
    @Test
    public void restCountStopsAtTheTerminator() {
        // 2 glyphs on the first line, then "cd " (3) before &c ends the ramp: 5 total.
        GradientWrap.Carry carry = GradientWrap.carryAcrossBreak("&g&#ff0000&#0000ffab", "cd &cxx");

        int boundary = TextEffectMath.computeGradientColor(0xFF0000, 0x0000FF, 2, 5);
        assertEquals(String.format("&g&#%06x&#0000ff", boundary), carry.continuationToken);
    }

    @Test
    public void plainTextHasNothingToCarry() {
        assertNull(GradientWrap.carryAcrossBreak("no gradient here", "more text"));
        assertNull(GradientWrap.carryAcrossBreak("&cred text", "more"));
    }

    /** Styles ahead of the token survive into the extracted prefix alongside it. */
    @Test
    public void extractorKeepsStylesThroughAGradientToken() {
        String prefix = StringUtils.extractFormatFromString("&l&g&#ff0000&#0000ffbold gradient");

        assertEquals("&g&#ff0000&#0000ff&l", prefix);
    }
}
