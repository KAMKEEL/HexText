package kamkeel.hextext.common.util;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.config.HexTextConfig;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

public class StringUtilsTest {

    @Before
    public void setUp() {
        HexText.proxy = new CommonProxy();
        HexTextConfig.resetToDefaults();
        // These cover HexText's own reset rules; the shipped default carries styles.
        HexTextConfig.setVanillaReset(true);
        HexTextConfig.setUniversalAmpersandEnabled(true);
        HexTextConfig.setEnableRgbHtmlFormat(true);
    }

    /**
     * An escaped code is the reader's to see: the send conversion leaves the pair
     * alone - backslash and all - so the renderer's escape handling can show the
     * bare code. Converting it painted the text and stranded the backslash.
     */
    @Test
    public void sendConversionLeavesEscapedCodesAlone() {
        org.junit.Assert.assertEquals("\\&c literal", StringUtils.convertAmpersandsToSectionSigns("\\&c literal"));
        org.junit.Assert.assertEquals("\\&#FF0000literal", StringUtils.convertAmpersandsToSectionSigns("\\&#FF0000literal"));
        org.junit.Assert.assertEquals("§cred \\&a not green §agreen",
            StringUtils.convertAmpersandsToSectionSigns("&cred \\&a not green &agreen"));
        org.junit.Assert.assertEquals("\\&q\\&z\\&v\\&u",
            StringUtils.convertAmpersandsToSectionSigns("\\&q\\&z\\&v\\&u"));
    }

    @Test
    public void extractFormatKeepsLatestColour() {
        String input = "&a&lBold<123456>Still";
        String prefix = StringUtils.extractFormatFromString(input);
        assertEquals("<123456>", prefix);
    }

    @Test
    public void extractFormatHandlesDirectRgbCode() {
        String input = "&#112233Hello";
        String prefix = StringUtils.extractFormatFromString(input);
        assertEquals("&#112233", prefix);
    }

    @Test
    public void extractFormatHandlesSectionSignRgbCode() {
        String input = "§#445566Hello";
        String prefix = StringUtils.extractFormatFromString(input);
        assertEquals("§#445566", prefix);
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
    public void convertAmpersandsToSectionSignsReplacesRecognisedTokens() {
        String converted = StringUtils.convertAmpersandsToSectionSigns("&a&lHello &#A1B2C3World");
        assertEquals("§a§lHello §#A1B2C3World", converted);
    }

    @Test
    public void convertAmpersandsToSectionSignsLeavesInvalidAmpersands() {
        String converted = StringUtils.convertAmpersandsToSectionSigns("&x& Hello &");
        assertEquals("&x& Hello &", converted);
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
    public void stripColorsRemovesMinecraftCodes() {
        String input = "&a&lHello §cWorld";
        assertEquals("&lHello World", StringUtils.stripColors(input));
    }

    @Test
    public void stripColorsKeepsHexAndStyles() {
        String input = "&#123456&lColourful";
        assertEquals("&#123456&lColourful", StringUtils.stripColors(input));
    }

    @Test
    public void stripColorsRemovesRainbowCode() {
        String input = "&gRainbow";
        assertEquals("Rainbow", StringUtils.stripColors(input));
    }

    @Test
    public void stripStylesKeepsColours() {
        String input = "&a&lBold&o Text";
        assertEquals("&aBold Text", StringUtils.stripStyles(input));
    }

    @Test
    public void stripHexColorsRemovesDirectRgbCodes() {
        String input = "&#abcdefColour";
        assertEquals("Colour", StringUtils.stripHexColors(input));
    }

    @Test
    public void containsFormattingCodesDetectsTokens() {
        assertTrue(StringUtils.containsFormattingCodes("Plain &aColour"));
        assertFalse(StringUtils.containsFormattingCodes("Plain text"));
    }
}
