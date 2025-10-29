package kamkeel.hextext.client;

import kamkeel.hextext.client.support.SimpleCharWidthFunction;
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
    public void computeLineBreakMatchesVanillaWithBold() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        widthFunction.setWidth(' ', 4.0f);
        String text = "\u00A7lHello world";
        int maxWidth = 30;

        int expected = vanillaSizeStringToWidth(text, maxWidth, widthFunction);
        int actual = FormattedTextMetrics.computeLineBreakIndex(text, maxWidth, false, widthFunction, 0.0f, 1.0f);

        assertEquals(expected, actual);
    }

    @Test
    public void computeLineBreakMatchesVanillaWhenBoldResets() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        widthFunction.setWidth(' ', 4.0f);
        String text = "\u00A7lHello \u00A7cworld";
        int maxWidth = 34;

        int expected = vanillaSizeStringToWidth(text, maxWidth, widthFunction);
        int actual = FormattedTextMetrics.computeLineBreakIndex(text, maxWidth, false, widthFunction, 0.0f, 1.0f);

        assertEquals(expected, actual);
    }

    @Test
    public void computeLineBreakCountsSectionSignFormattingBytes() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(6.0f);
        String text = "\u00A7lAB";

        int expected = vanillaSizeStringToWidth(text, 5, widthFunction);
        int actual = FormattedTextMetrics.computeLineBreakIndex(text, 5, false, widthFunction, 0.0f, 1.0f);

        assertEquals(expected, actual);
    }

    @Test
    public void computeLineBreakHandlesAmpersandFormatting() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        widthFunction.setWidth(' ', 4.0f);
        String text = "&lAB &oCD";

        int breakIndex = FormattedTextMetrics.computeLineBreakIndex(text, 12, false, widthFunction, 0.0f, 1.0f);

        assertEquals(4, breakIndex);
    }

    @Test
    public void computeLineBreakIncludesHexColorLength() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        String text = "&123456AB";

        int breakIndex = FormattedTextMetrics.computeLineBreakIndex(text, 4, false, widthFunction, 0.0f, 1.0f);

        assertEquals(7, breakIndex);
    }

    @Test
    public void computeLineBreakIncludesAngleBracketRgbLength() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        String text = "<123456>AB";

        int breakIndex = FormattedTextMetrics.computeLineBreakIndex(text, 4, false, widthFunction, 0.0f, 1.0f);

        assertEquals(8, breakIndex);
    }

    @Test
    public void computeLineBreakCountsClosingAngleBracketRgbLength() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        String text = "<123456>AB</123456>C";

        int breakIndex = FormattedTextMetrics.computeLineBreakIndex(text, 12, false, widthFunction, 0.0f, 1.0f);

        assertEquals(19, breakIndex);
    }

    @Test
    public void computeLineBreakIgnoresFormattingInRawMode() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        String text = "&lAB";

        int breakIndex = FormattedTextMetrics.computeLineBreakIndex(text, 5, true, widthFunction, 0.0f, 1.0f);

        assertEquals(1, breakIndex);
    }

    @Test
    public void calculateWidthIgnoresRgbTags() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(6.0f);
        float width = FormattedTextMetrics.calculateMaxLineWidth("<123456>AB</123456>", false, widthFunction, 0.0f, 1.0f);

        assertEquals(12.0f, width, 0.0001f);
    }

    @Test
    public void rgbColorResetsBoldWidth() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        float width = FormattedTextMetrics.calculateMaxLineWidth("&lA&123456B", false, widthFunction, 0.0f, 1.0f);

        assertEquals(11.0f, width, 0.0001f);
    }

    private static int vanillaSizeStringToWidth(CharSequence text, int maxWidth,
            SimpleCharWidthFunction widthFunction) {
        int length = text.length();
        int width = 0;
        int index = 0;
        int lastSpace = -1;
        boolean bold = false;

        while (index < length) {
            char character = text.charAt(index);

            if (character == '\n') {
                break;
            }

            if (character == '\u00A7' && index + 1 < length) {
                char format = Character.toLowerCase(text.charAt(++index));
                if (format == 'l') {
                    bold = true;
                } else if (format == 'r' || (format >= '0' && format <= '9') || (format >= 'a' && format <= 'f')) {
                    bold = false;
                }
                index++;
                continue;
            }

            if (character == ' ') {
                lastSpace = index;
            }

            int charWidth = Math.max(0, Math.round(widthFunction.getWidth(character)));
            width += charWidth;
            if (bold && charWidth > 0) {
                width += 1;
            }

            if (width > maxWidth) {
                break;
            }

            index++;
        }

        if (index != length && lastSpace != -1 && lastSpace < index) {
            return lastSpace;
        }

        return index;
    }
}
