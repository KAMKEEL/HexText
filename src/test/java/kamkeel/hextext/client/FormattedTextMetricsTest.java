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
    public void computeLineBreakMatchesVanillaWithCarriedFormatting() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        widthFunction.setWidth(' ', 4.0f);
        String text = "\u00A7l\u00A7oWide words here";
        int maxWidth = 28;

        int expected = vanillaSizeStringToWidth(text, maxWidth, widthFunction);
        int actual = FormattedTextMetrics.computeLineBreakIndex(text, maxWidth, false, widthFunction, 0.0f, 1.0f);

        assertEquals(expected, actual);
    }

    @Test
    public void computeLineBreakTreatsRainbowAsColourReset() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        int maxWidth = 19;
        String rainbow = "&lAB&gCD";
        String reset = "&lAB&rCD";

        int rainbowBreak = FormattedTextMetrics.computeLineBreakIndex(rainbow, maxWidth, false, widthFunction, 0.0f, 1.0f);
        int resetBreak = FormattedTextMetrics.computeLineBreakIndex(reset, maxWidth, false, widthFunction, 0.0f, 1.0f);

        assertEquals(visibleLength(reset, resetBreak), visibleLength(rainbow, rainbowBreak));
    }

    @Test
    public void computeLineBreakTreatsRgbTagsAsColourReset() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        int maxWidth = 19;
        String rgb = "&lAB<123456>CD";
        String reset = "&lAB&rCD";

        int rgbBreak = FormattedTextMetrics.computeLineBreakIndex(rgb, maxWidth, false, widthFunction, 0.0f, 1.0f);
        int resetBreak = FormattedTextMetrics.computeLineBreakIndex(reset, maxWidth, false, widthFunction, 0.0f, 1.0f);

        assertEquals(visibleLength(reset, resetBreak), visibleLength(rgb, rgbBreak));
    }

    @Test
    public void computeLineBreakIgnoresFormattingInRawMode() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        widthFunction.setWidth(' ', 3.0f);
        widthFunction.setWidth('#', 5.0f);
        String text = "&lBold Raw";
        int maxWidth = 18;

        int rawIndex = FormattedTextMetrics.computeLineBreakIndex(text, maxWidth, true, widthFunction, 0.0f, 1.0f);
        int expected = FormattedTextMetrics.computeLineBreakIndex(text.replace('&', '#'), maxWidth,
            false, widthFunction, 0.0f, 1.0f);

        assertEquals(expected, rawIndex);
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

    private static int visibleLength(String text, int breakIndex) {
        int limit = Math.min(breakIndex, text.length());
        int visible = 0;
        for (int i = 0; i < limit; ) {
            int codeLen = ColorCodeUtils.detectColorCodeLengthIgnoringRaw(text, i);
            if (codeLen > 0) {
                i += codeLen;
                continue;
            }
            if (text.charAt(i) != '\n') {
                visible++;
            }
            i++;
        }
        return visible;
    }
}
