package kamkeel.hextext.common.util;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.config.HexTextConfig;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

public class ColorCodeUtilsTest {

    @Before
    public void setUp() {
        HexText.proxy = new CommonProxy();
        HexTextConfig.resetToDefaults();
        HexTextConfig.setUniversalAmpersandEnabled(true);
        HexTextConfig.setEnableRgbHtmlFormat(true);
    }

    @Test
    public void testValidHexChar() {
        assertTrue(ColorCodeUtils.isValidHexChar('a'));
        assertTrue(ColorCodeUtils.isValidHexChar('F'));
        assertFalse(ColorCodeUtils.isValidHexChar('g'));
        assertFalse(ColorCodeUtils.isValidHexChar(' '));
    }

    @Test
    public void testParseHexColor() {
        assertEquals(0xABCDEF, ColorCodeUtils.parseHexColor("ABCDEF"));
        assertEquals(-1, ColorCodeUtils.parseHexColor("XYZ123"));
    }

    @Test
    public void testMinecraftColorIndex() {
        assertEquals(0, ColorCodeUtils.getMinecraftColorIndex('0'));
        assertEquals(9, ColorCodeUtils.getMinecraftColorIndex('9'));
        assertEquals(10, ColorCodeUtils.getMinecraftColorIndex('a'));
        assertEquals(15, ColorCodeUtils.getMinecraftColorIndex('F'));
        assertEquals(-1, ColorCodeUtils.getMinecraftColorIndex('g'));
    }

    @Test
    public void testDetectColorCodeLength() {
        assertEquals(8, ColorCodeUtils.detectColorCodeLength("&#123456rest", 0));
        assertEquals(2, ColorCodeUtils.detectColorCodeLength("§ares", 0));
        assertEquals(9, ColorCodeUtils.detectColorCodeLength("</ABCDEF>xyz", 0));
        assertEquals(8, ColorCodeUtils.detectColorCodeLength("<ABCDEF>xyz", 0));
        assertEquals(0, ColorCodeUtils.detectColorCodeLength("plain", 0));
    }

    @Test
    public void testDetectColorCodeLengthRespectsHtmlToggle() {
        HexTextConfig.setEnableRgbHtmlFormat(false);
        assertEquals(0, ColorCodeUtils.detectColorCodeLength("<ABCDEF>xyz", 0));
        assertEquals(0, ColorCodeUtils.detectColorCodeLength("</ABCDEF>xyz", 0));
    }

    @Test
    public void testDetectColorCodeLengthRespectsAmpersandToggle() {
        HexTextConfig.setUniversalAmpersandEnabled(false);
        assertEquals(0, ColorCodeUtils.detectColorCodeLength("&#123456rest", 0));
        assertEquals(0, ColorCodeUtils.detectColorCodeLength("&aColour", 0));
    }

    @Test
    public void testCalculateShadowColor() {
        assertEquals(0x1E1E1E, ColorCodeUtils.calculateShadowColor(0x7A7A7A));
    }

    @Test
    public void testContainsFormattingCodes() {
        assertTrue(ColorCodeUtils.containsFormattingCodes("Hello &#aabbccWorld"));
        assertFalse(ColorCodeUtils.containsFormattingCodes("Plain text"));
    }

    @Test
    public void testIndexOfNextFormattingCode() {
        assertEquals(6, ColorCodeUtils.indexOfNextFormattingCode("Hello &aWorld", 0));
        assertEquals(-1, ColorCodeUtils.indexOfNextFormattingCode("Plain", 0));
        assertEquals(-1, ColorCodeUtils.indexOfNextFormattingCode("Hello &aWorld", 20));
    }

    @Test
    public void testDetectAmpersandFormattingCodeLength() {
        assertEquals(2, ColorCodeUtils.detectAmpersandFormattingCodeLength("&a", 0));
        assertEquals(8, ColorCodeUtils.detectAmpersandFormattingCodeLength("&#ABCDEF", 0));
        assertEquals(0, ColorCodeUtils.detectAmpersandFormattingCodeLength("&x", 0));
        assertEquals(0, ColorCodeUtils.detectAmpersandFormattingCodeLength("&", 0));
    }

    @Test
    public void testTokenClassificationHelpers() {
        assertTrue(ColorCodeUtils.isStandardColorToken("&a", 0, 2));
        assertFalse(ColorCodeUtils.isStandardColorToken("&x", 0, 2));
        assertTrue(ColorCodeUtils.isHexColorToken("&#123456", 0, 8));
        assertTrue(ColorCodeUtils.isHexColorToken("<123456>", 0, 8));
        assertTrue(ColorCodeUtils.isHexColorToken("</123456>", 0, 9));
        assertTrue(ColorCodeUtils.isStyleOrEffectToken("&l", 0, 2));
        assertTrue(ColorCodeUtils.isStyleOrEffectToken("&j", 0, 2));
        assertFalse(ColorCodeUtils.isStyleOrEffectToken("&r", 0, 2));
    }
}
