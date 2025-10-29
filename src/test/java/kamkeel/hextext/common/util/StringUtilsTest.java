package kamkeel.hextext.common.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilsTest {

    @Test
    public void extractFormatKeepsLatestColour() {
        String input = "&a&lBold<123456>Still";
        String prefix = StringUtils.extractFormatFromString(input);
        assertEquals("<123456>", prefix);
    }

    @Test
    public void extractFormatClearsOnReset() {
        String input = "&aGreen&rPlain";
        String prefix = StringUtils.extractFormatFromString(input);
        assertTrue(prefix.isEmpty());
    }

    @Test
    public void extractFormatPreservesStyles() {
        String input = "&a&lBold";
        String prefix = StringUtils.extractFormatFromString(input);
        assertEquals("&a&l", prefix);
    }

    @Test
    public void extractFormatIncludesEffects() {
        String input = "&e&o&jHello";
        String prefix = StringUtils.extractFormatFromString(input);
        assertEquals("&e&o&j", prefix);
    }

    @Test
    public void extractFormatTreatsRainbowAsColour() {
        String input = "&g&iFlicker";
        String prefix = StringUtils.extractFormatFromString(input);
        assertEquals("&g&i", prefix);
    }

    @Test
    public void normalizeForRawDisplayConvertsSectionSigns() {
        String normalized = StringUtils.normalizeForRawDisplay("§aHello §rWorld");
        assertEquals("&aHello &rWorld", normalized);
    }

    @Test
    public void stripExtrasRemovesAllFormatting() {
        String input = "&a&lBold <123456>Text&n";
        assertEquals("Bold Text", StringUtils.stripExtras(input));
    }

    @Test
    public void stripHexColorsPreservesStyles() {
        String input = "&a&lHello <123456>World</123456>";
        assertEquals("&lHello World", StringUtils.stripHexColors(input));
    }

    @Test
    public void stripStylesKeepsColours() {
        String input = "&a&lBold&o Text";
        assertEquals("&aBold Text", StringUtils.stripStyles(input));
    }

    @Test
    public void containsFormattingCodesDetectsTokens() {
        assertTrue(StringUtils.containsFormattingCodes("Plain &aColour"));
        assertFalse(StringUtils.containsFormattingCodes("Plain text"));
    }
}
