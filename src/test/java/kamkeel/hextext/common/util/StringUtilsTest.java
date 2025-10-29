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
    public void stripColorCodesRemovesMinecraftFormatting() {
        String input = "&aGreen <#FFAA00>and &lBold";
        String result = StringUtils.stripColorCodes(input);
        assertEquals("Green and Bold", result);
    }

    @Test
    public void containsColorCodesDetectsHexAndLegacyCodes() {
        assertTrue(StringUtils.containsColorCodes("<#123456>Fancy"));
        assertTrue(StringUtils.containsColorCodes("&aHello"));
        assertFalse(StringUtils.containsColorCodes("No formatting here"));
    }

    @Test
    public void stripExtrasRemovesStraySectionCharacters() {
        String input = "Text with stray " + (char) 167 + " section";
        String result = StringUtils.stripExtras(input);
        assertEquals("Text with stray  section", result);
    }
}
