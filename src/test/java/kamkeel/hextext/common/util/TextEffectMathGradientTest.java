package kamkeel.hextext.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The gradient walks HSV along the shorter hue arc. The expected values here are
 * duplicated in Angelica's ColorCodeUtilsTest on the angelica-compat branch - the
 * two renderers must colour the same glyph of the same string identically, and
 * these pins are what holds the pair together.
 */
public class TextEffectMathGradientTest {

    @Test
    public void endpointsAreExact() {
        assertEquals(0xFF0000, TextEffectMath.computeGradientColor(0xFF0000, 0x0000FF, 0, 11));
        assertEquals(0x0000FF, TextEffectMath.computeGradientColor(0xFF0000, 0x0000FF, 10, 11));
    }

    /** Red to blue turns through magenta, not through dark purple. */
    @Test
    public void redToBlueMidpointIsMagenta() {
        assertEquals(0xFF00FF, TextEffectMath.computeGradientColor(0xFF0000, 0x0000FF, 5, 11));
    }

    /** A fade to white keeps its hue and just desaturates. */
    @Test
    public void fadeToWhiteStaysOnItsHue() {
        int mid = TextEffectMath.computeGradientColor(0xFF0000, 0xFFFFFF, 5, 11);
        assertEquals(0xFF7F7F, mid);
    }

    /** A span of one glyph is the start colour; there is nowhere to travel. */
    @Test
    public void singleGlyphSpanIsTheStartColour() {
        assertEquals(0xFF0000, TextEffectMath.computeGradientColor(0xFF0000, 0x0000FF, 0, 1));
    }

    @Test
    public void yellowToMagentaStaysBright() {
        // Hue 60 to hue 300 - the short arc runs backwards through red (hue 0).
        assertEquals(0xFF0000, TextEffectMath.computeGradientColor(0xFFFF00, 0xFF00FF, 5, 11));
    }
}
