package kamkeel.hextext.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FormattedTextMetricsTest {

    private static final FormattedTextMetrics.CharWidthFunction MONOSPACE = new FormattedTextMetrics.CharWidthFunction() {
        @Override
        public float getWidth(char character) {
            return 5.0f;
        }
    };

    @Test
    public void testEffectCodesDoNotAffectWidth() {
        String text = "&gRainbow &hFlip";
        float width = FormattedTextMetrics.calculateMaxLineWidth(text, false, MONOSPACE, 0.0f, 1.0f);
        int printableCharacters = "Rainbow Flip".length();
        assertEquals(printableCharacters * 5.0f, width, 0.0001f);
    }

    @Test
    public void testBoldFormattingAddsExtraWidth() {
        String text = "&lAB";
        float width = FormattedTextMetrics.calculateMaxLineWidth(text, false, MONOSPACE, 0.0f, 1.0f);
        float expected = (5.0f + 1.0f) * 2;
        assertEquals(expected, width, 0.0001f);
    }

    @Test
    public void testLineBreakComputationSkipsEffectCodes() {
        String text = "&gABCDEF";
        int breakIndex = FormattedTextMetrics.computeLineBreakIndex(text, 15, false, MONOSPACE, 0.0f, 1.0f);
        assertEquals("Expected break after three printable characters", 2 + 3, breakIndex);
    }
}
