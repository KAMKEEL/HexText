package kamkeel.hextext.client;

import kamkeel.hextext.client.support.SimpleCharWidthFunction;
import kamkeel.hextext.util.ColorCodeUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FormattedTextMetricsTest {

    @Test
    public void calculateMaxLineWidthSkipsFormattingCodes() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        float width = FormattedTextMetrics.calculateMaxLineWidth("&aAB", false, widthFunction, 0.0f, 1.0f);
        assertEquals(10.0f, width, 0.0001f);
    }

    @Test
    public void calculateMaxLineWidthAccountsForBold() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        float width = FormattedTextMetrics.calculateMaxLineWidth("&lAB&rC", false, widthFunction, 0.0f, 1.0f);
        assertEquals(17.0f, width, 0.0001f);
    }

    @Test
    public void computeLineBreakHonoursSafePositions() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        widthFunction.setWidth(' ', 2.0f);
        int breakIndex = FormattedTextMetrics.computeLineBreakIndex("AB CD", 12, false, widthFunction, 0.0f, 1.0f);
        assertEquals(2, breakIndex);
    }

    @Test
    public void formattingCodesDoNotIncreaseWidth() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        float width = FormattedTextMetrics.calculateMaxLineWidth("&hFlip&i&j", false, widthFunction, 0.0f, 1.0f);
        assertEquals(20.0f, width, 0.0001f);
    }

    @Test
    public void rainbowCodeResetsBoldWidth() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        float width = FormattedTextMetrics.calculateMaxLineWidth("&lA&gBC", false, widthFunction, 0.0f, 1.0f);
        assertEquals(16.0f, width, 0.0001f);
    }

    @Test
    public void rgbPushResetsBoldWidth() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        float width = FormattedTextMetrics.calculateMaxLineWidth("&lA<123456>BC", false, widthFunction, 0.0f, 1.0f);
        assertEquals(16.0f, width, 0.0001f);
    }

    @Test
    public void sizeStringToWidthMatchesVanillaBoldBehaviour() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        widthFunction.setWidth(' ', 3.0f);
        String text = "\u00a7lHELLO WORLD";

        int expected = referenceSizeStringToWidth(text, 40, widthFunction);
        int actual = FormattedTextMetrics.computeLineBreakIndex(text, 40, false,
            widthFunction, 0.0f, 1.0f);

        assertEquals("bold-aware width mismatch", expected, actual);
    }

    private static int referenceSizeStringToWidth(CharSequence text, int maxWidth,
            SimpleCharWidthFunction widths) {
        int length = text.length();
        int width = 0;
        int index = 0;
        int lastSpace = -1;
        boolean bold = false;

        while (index < length) {
            char current = text.charAt(index);

            if (current == '\u00a7' && index + 1 < length) {
                char fmt = text.charAt(++index);
                if (fmt == 'l' || fmt == 'L') {
                    bold = true;
                } else if (fmt == 'r' || fmt == 'R'
                    || ColorCodeUtils.isMinecraftColorCode(fmt)) {
                    bold = false;
                }
                index++;
                continue;
            }

            if (current == '\n') {
                return index;
            }

            if (current == ' ') {
                lastSpace = index;
            }

            float charWidth = widths.getWidth(current);
            width += charWidth;
            if (bold && charWidth > 0) {
                width += 1.0f;
            }

            if (width > maxWidth) {
                return lastSpace != -1 && lastSpace < index ? lastSpace : index;
            }

            index++;
        }

        return length;
    }
}
