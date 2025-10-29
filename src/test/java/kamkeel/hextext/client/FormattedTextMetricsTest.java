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
    public void trimStringToWidthMatchesVanillaWithBold() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        widthFunction.setWidth(' ', 4.0f);
        String text = "\u00A7lHello world";
        int maxWidth = 30;

        String expected = vanillaTrimStringToWidth(text, maxWidth, widthFunction);
        String actual = FormattedTextMetrics.trimStringToWidth(text, maxWidth, false, widthFunction, 0.0f, 1.0f);

        assertEquals(expected, actual);
    }

    @Test
    public void trimStringToWidthKeepsSpaceWhenWithinLimit() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        widthFunction.setWidth(' ', 4.0f);
        String text = "Hello world";
        int maxWidth = 29; // allows "Hello " but not the following 'w'

        String actual = FormattedTextMetrics.trimStringToWidth(text, maxWidth, false, widthFunction, 0.0f, 1.0f);

        assertEquals("Hello ", actual);
    }

    @Test
    public void trimStringToWidthHonoursAmpersandFormatting() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        widthFunction.setWidth(' ', 4.0f);
        String text = "&lHello &cworld";
        int maxWidth = 35;

        String actual = FormattedTextMetrics.trimStringToWidth(text, maxWidth, false, widthFunction, 0.0f, 1.0f);

        assertEquals("&lHello ", actual);
    }

    @Test
    public void trimStringToWidthTreatsAmpersandLiterallyInRawMode() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        widthFunction.setWidth('&', 3.0f);
        widthFunction.setWidth('l', 2.0f);
        String text = "&lHi";

        String actual = FormattedTextMetrics.trimStringToWidth(text, 15, true, widthFunction, 0.0f, 1.0f);

        assertEquals("&lHi", actual);
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

    private static String vanillaTrimStringToWidth(CharSequence text, int maxWidth,
            SimpleCharWidthFunction widthFunction) {
        StringBuilder builder = new StringBuilder();
        int width = 0;
        boolean expectingFormatCode = false;
        boolean bold = false;
        final int length = text.length();

        for (int index = 0; index < length && width < maxWidth; index++) {
            char character = text.charAt(index);

            if (expectingFormatCode) {
                expectingFormatCode = false;
                builder.append(character);
                char fmt = Character.toLowerCase(character);
                if (fmt == 'l') {
                    bold = true;
                } else if (fmt == 'r' || (fmt >= '0' && fmt <= '9') || (fmt >= 'a' && fmt <= 'f')) {
                    bold = false;
                }
                continue;
            }

            if (character == '\u00A7') {
                expectingFormatCode = true;
                builder.append(character);
                continue;
            }

            int charWidth = Math.max(0, Math.round(widthFunction.getWidth(character)));
            int nextWidth = width + charWidth;
            if (bold) {
                nextWidth += 1;
            }

            if (nextWidth > maxWidth) {
                break;
            }

            builder.append(character);
            width = nextWidth;
        }

        return builder.toString();
    }
}
