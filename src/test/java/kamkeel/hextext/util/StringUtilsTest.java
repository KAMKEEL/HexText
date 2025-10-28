package kamkeel.hextext.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilsTest {

    @Test
    public void testNormalizeForRawDisplay() {
        assertEquals("&aHello", StringUtils.normalizeForRawDisplay("§aHello"));
        assertEquals("plain", StringUtils.normalizeForRawDisplay("plain"));
    }

    @Test
    public void testExtractFormatFromString() {
        String colorOnly = "&aHello";
        assertEquals("&a", StringUtils.extractFormatFromString(colorOnly));

        String input = "&bTest &lWorld";
        assertEquals("&b&l", StringUtils.extractFormatFromString(input));

        String nested = "<ABCDEF>Color</ABCDEF> &nUnder";
        assertEquals("&n", StringUtils.extractFormatFromString(nested));
    }

    @Test
    public void testStripColorCodes() {
        String input = "&aHello<ABCDEF>World</ABCDEF>&r!";
        assertEquals("HelloWorld!", StringUtils.stripColorCodes(input));
    }
}
