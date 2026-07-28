package kamkeel.hextext.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * {@code §x§R§R§G§G§B§B} is how most of the ecosystem spells an RGB colour, and until
 * HexText read it too a string written that way rendered as a colour only where
 * something else understood it. Under HexText's own renderer the six {@code §<nibble>}
 * pairs were six vanilla colour codes in a row, and the last nibble won - which is the
 * same failure, from the other side, as the one that started all of this.
 */
public class SectionXTest {

    @Test
    public void readsTheColourOutOfTheNibbles() {
        assertEquals(0xFF0000, ColorCodeUtils.parseSectionX("§x§f§f§0§0§0§0", 0));
        assertEquals(0x123123, ColorCodeUtils.parseSectionX("§x§1§2§3§1§2§3", 0));
        assertEquals(0x000000, ColorCodeUtils.parseSectionX("§x§0§0§0§0§0§0", 0));
        assertEquals(0xFFFFFF, ColorCodeUtils.parseSectionX("§X§F§F§F§F§F§F", 0));
    }

    @Test
    public void readsItPartWayThroughAString() {
        assertEquals(0x00FF00, ColorCodeUtils.parseSectionX("hello §x§0§0§f§f§0§0there", 6));
    }

    @Test
    public void refusesAnythingThatIsNotOne() {
        assertEquals(-1, ColorCodeUtils.parseSectionX("§x§f§f§0§0§0", 0));      // too short
        assertEquals(-1, ColorCodeUtils.parseSectionX("§x§f§f§0§0§0§g", 0));    // not hex
        assertEquals(-1, ColorCodeUtils.parseSectionX("§xff0000000000", 0));    // no separators
        assertEquals(-1, ColorCodeUtils.parseSectionX("&x&f&f&0&0&0&0", 0));    // ampersand form
        assertEquals(-1, ColorCodeUtils.parseSectionX("§c§f§f§0§0§0§0", 0));    // not x
        assertEquals(-1, ColorCodeUtils.parseSectionX(null, 0));
    }

    /** Detection must claim all fourteen, or the tail is read as colours of its own. */
    @Test
    public void detectionClaimsTheWholeToken() {
        assertEquals(ColorCodeUtils.SECTION_X_LENGTH,
            ColorCodeUtils.detectColorCodeLength("§x§f§f§0§0§0§0text", 0));
        // A plain §c is still two, and §# is still eight.
        assertEquals(2, ColorCodeUtils.detectColorCodeLength("§ctext", 0));
        assertEquals(8, ColorCodeUtils.detectColorCodeLength("§#FF0000text", 0));
    }
}
