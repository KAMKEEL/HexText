package kamkeel.hextext.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** The two new effects' maths, which are pure and so can be pinned exactly. */
public class WaveAndGradientMathTest {

    private static final float FREQ = 0.6f;
    private static final float AMP = 1.5f;

    @Test
    public void aFlatWaveDoesNotMove() {
        assertEquals(0.0f, TextEffectMath.computeWaveOffset(1234L, 5, 1000L, FREQ, 0.0f), 0.0001f);
    }

    @Test
    public void theWaveStaysInsideItsAmplitude() {
        for (long now = 0; now < 2000; now += 37) {
            for (int i = 0; i < 24; i++) {
                float offset = TextEffectMath.computeWaveOffset(now, i, 1000L, FREQ, AMP);
                assertTrue("escaped amplitude: " + offset, Math.abs(offset) <= AMP + 0.0001f);
            }
        }
    }

    /** Neighbouring glyphs sit at different phases, or it is a rigid bar going up and down. */
    @Test
    public void neighbouringGlyphsAreOutOfPhase() {
        float a = TextEffectMath.computeWaveOffset(0L, 0, 1000L, FREQ, AMP);
        float b = TextEffectMath.computeWaveOffset(0L, 1, 1000L, FREQ, AMP);
        assertTrue("glyphs moved together", Math.abs(a - b) > 0.0001f);
    }

    /** And it travels: the same glyph is elsewhere a moment later. */
    @Test
    public void theWaveMovesOverTime() {
        float then = TextEffectMath.computeWaveOffset(0L, 3, 1000L, FREQ, AMP);
        float later = TextEffectMath.computeWaveOffset(250L, 3, 1000L, FREQ, AMP);
        assertTrue("wave stood still", Math.abs(then - later) > 0.0001f);
    }

    /** A cycle returns to where it began. */
    @Test
    public void theWaveRepeats() {
        assertEquals(
            TextEffectMath.computeWaveOffset(0L, 7, 1000L, FREQ, AMP),
            TextEffectMath.computeWaveOffset(1000L, 7, 1000L, FREQ, AMP), 0.0001f);
    }

    @Test
    public void aGradientStartsAndEndsOnItsColours() {
        assertEquals(0xFF0000, TextEffectMath.computeGradientColor(0xFF0000, 0x0000FF, 0, 10));
        assertEquals(0x0000FF, TextEffectMath.computeGradientColor(0xFF0000, 0x0000FF, 9, 10));
    }

    @Test
    public void aGradientMeetsInTheMiddle() {
        // Black to white is a pure value fade - no hue on either end - and the
        // conversion truncates, so the midpoint sits at 0x7F rather than 0x80.
        assertEquals(0x7F7F7F, TextEffectMath.computeGradientColor(0x000000, 0xFFFFFF, 1, 3));
    }

    /** One glyph has nowhere to travel, and must not divide by the span. */
    @Test
    public void aSingleGlyphGradientIsTheStartColour() {
        assertEquals(0xFF0000, TextEffectMath.computeGradientColor(0xFF0000, 0x0000FF, 0, 1));
        assertEquals(0xFF0000, TextEffectMath.computeGradientColor(0xFF0000, 0x0000FF, 0, 0));
    }

    /** An index past either end clamps rather than running off the gradient. */
    @Test
    public void indicesOutsideTheSpanClamp() {
        assertEquals(0xFF0000, TextEffectMath.computeGradientColor(0xFF0000, 0x0000FF, -4, 10));
        assertEquals(0x0000FF, TextEffectMath.computeGradientColor(0xFF0000, 0x0000FF, 40, 10));
    }

    /**
     * The ramp turns through hues at full brightness rather than blending channels
     * into grey. Opposite hues are a tie for which way round; the tie-break is the
     * falling arc, and it has to stay deterministic because Angelica's gradient
     * breaks the same tie the same way.
     */
    @Test
    public void oppositeHuesStayVividThroughTheTurn() {
        // Magenta (300°) to green (120°): the falling arc passes blue; halfway is
        // azure at full saturation and value, not a grey blend.
        assertEquals(0x007FFF, TextEffectMath.computeGradientColor(0xFF00FF, 0x00FF00, 1, 3));
    }
}
