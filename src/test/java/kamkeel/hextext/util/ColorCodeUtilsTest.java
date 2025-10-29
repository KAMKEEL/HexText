package kamkeel.hextext.util;

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
    public void detectLegacyFormattingCodeLength() {
        assertEquals(2, ColorCodeUtils.detectColorCodeLength("&l", 0));
    }

    @Test
    public void detectInlineHexColorLength() {
        assertEquals(7, ColorCodeUtils.detectColorCodeLength("&123abc", 0));
        assertEquals(2, ColorCodeUtils.detectColorCodeLength("§ares", 0));
    }

    @Test
    public void detectTaggedHexColorLengths() {
        assertEquals(8, ColorCodeUtils.detectColorCodeLength("<abcdef>", 0));
        assertEquals(9, ColorCodeUtils.detectColorCodeLength("</abcdef>", 0));
        assertEquals(9, ColorCodeUtils.detectColorCodeLength("</ABCDEF>xyz", 0));
        assertEquals(8, ColorCodeUtils.detectColorCodeLength("<ABCDEF>xyz", 0));
    }

    @Test
    public void rawModeDisablesDetection() {
        assertEquals(0, ColorCodeUtils.detectColorCodeLength("&123abc", 0, true));
    }

    @Test
    public void validatesHexStrings() {
        assertTrue(ColorCodeUtils.isValidHexString("a1b2c3"));
    }

    @Test
    public void detectPlainTextHasNoCode() {
        assertEquals(0, ColorCodeUtils.detectColorCodeLength("plain", 0));
    }

    @Test
    public void testCalculateShadowColor() {
        assertEquals(0x1E1E1E, ColorCodeUtils.calculateShadowColor(0x7A7A7A));
    }
}
