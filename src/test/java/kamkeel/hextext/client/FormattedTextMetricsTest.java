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
    public void computeLineBreakMatchesVanillaForRandomisedInputs() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(4.0f);
        widthFunction.setWidth(' ', 3.0f);
        widthFunction.setWidth('A', 6.0f);
        widthFunction.setWidth('B', 5.0f);
        widthFunction.setWidth('C', 7.0f);

        java.util.Random random = new java.util.Random(0xCAFE);
        char[] letters = new char[] {'A', 'B', 'C', ' '};
        char[] formatCodes = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'k', 'l', 'm', 'n', 'o', 'r'};

        for (int iteration = 0; iteration < 5_000; iteration++) {
            StringBuilder builder = new StringBuilder();
            int length = 5 + random.nextInt(20);

            for (int i = 0; i < length; i++) {
                int choice = random.nextInt(10);
                if (choice == 0) {
                    builder.append('\u00A7');
                    builder.append(formatCodes[random.nextInt(formatCodes.length)]);
                } else if (choice == 1) {
                    builder.append('\n');
                } else {
                    builder.append(letters[random.nextInt(letters.length)]);
                }
            }

            String text = builder.toString();
            int maxWidth = 5 + random.nextInt(40);

            int expected = vanillaSizeStringToWidth(text, maxWidth, widthFunction);
            int actual = FormattedTextMetrics.computeLineBreakIndex(text, maxWidth, false,
                widthFunction, 0.0f, 1.0f);

            assertEquals("Mismatch for text=" + text + " width=" + maxWidth, expected, actual);
        }
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
                lastSpace = index;
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
