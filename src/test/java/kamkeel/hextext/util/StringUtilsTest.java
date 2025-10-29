package kamkeel.hextext.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    public void convertLegacyFormattingCodesReplacesAmpersands() {
        String converted = StringUtils.convertLegacyFormattingCodes("&lBold &NA");
        assertEquals("§lBold §NA", converted);
    }

    @Test
    public void convertLegacyFormattingCodesIgnoresNonFormattingAmpersands() {
        String converted = StringUtils.convertLegacyFormattingCodes("Fish & Chips");
        assertEquals("Fish & Chips", converted);
    }
}
