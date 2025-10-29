package kamkeel.hextext.common.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class ColorCodeUtilsTest {

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
        assertEquals(7, ColorCodeUtils.detectColorCodeLength("&123456rest", 0));
        assertEquals(2, ColorCodeUtils.detectColorCodeLength("§ares", 0));
        assertEquals(9, ColorCodeUtils.detectColorCodeLength("</ABCDEF>xyz", 0));
        assertEquals(8, ColorCodeUtils.detectColorCodeLength("<ABCDEF>xyz", 0));
        assertEquals(0, ColorCodeUtils.detectColorCodeLength("plain", 0));
    }

    @Test
    public void testCalculateShadowColor() {
        assertEquals(0x1E1E1E, ColorCodeUtils.calculateShadowColor(0x7A7A7A));
    }

    @Test
    public void testContainsFormattingCodes() {
        assertTrue(ColorCodeUtils.containsFormattingCodes("Hello &aWorld"));
        assertFalse(ColorCodeUtils.containsFormattingCodes("Plain text"));
    }

    @Test
    public void testIndexOfNextFormattingCode() {
        assertEquals(6, ColorCodeUtils.indexOfNextFormattingCode("Hello &aWorld", 0));
        assertEquals(-1, ColorCodeUtils.indexOfNextFormattingCode("Plain", 0));
        assertEquals(-1, ColorCodeUtils.indexOfNextFormattingCode("Hello &aWorld", 20));
    }
}
