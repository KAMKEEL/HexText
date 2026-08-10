package kamkeel.hextext.common.render;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.common.util.ColorCodeUtils;
import kamkeel.hextext.common.util.TextEffectMath;
import kamkeel.hextext.config.HexTextConfig;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Codes HexText draws on its own, with no Angelica underneath. */
public class NativeCodeSupportTest {

    @Before
    public void setUp() {
        HexText.proxy = new CommonProxy();
        HexTextConfig.resetToDefaults();
        HexTextConfig.setUniversalAmpersandEnabled(true);
        HexTextConfig.setEnableRgbHtmlFormat(true);
    }

    /** q is a code of its own, so widths and conversions treat it as one. */
    @Test
    public void staticRainbowIsARecognisedCode() {
        assertTrue(ColorCodeUtils.isEffectCode('q'));
        assertEquals(2, ColorCodeUtils.detectAmpersandFormattingCodeLength("&qtext", 0));
    }

    /** The table matches Angelica's: 24 hues, one step per character. */
    @Test
    public void staticRainbowMatchesAngelicasTable() {
        assertEquals(ColorCodeUtils.hsvToRgb(0f, 1f, 1f), TextEffectMath.computeStaticRainbowColor(0, 0));
        assertEquals(ColorCodeUtils.hsvToRgb(15f, 1f, 1f), TextEffectMath.computeStaticRainbowColor(1, 0));
        assertEquals(ColorCodeUtils.hsvToRgb(345f, 1f, 1f), TextEffectMath.computeStaticRainbowColor(23, 0));
    }

    /** It wraps at the table's end and holds still, unlike the cycling one. */
    @Test
    public void staticRainbowWrapsAndDoesNotMove() {
        assertEquals(TextEffectMath.computeStaticRainbowColor(0, 0), TextEffectMath.computeStaticRainbowColor(24, 0));
        assertEquals(TextEffectMath.computeStaticRainbowColor(5, 0), TextEffectMath.computeStaticRainbowColor(29, 0));
    }

    /** Anchored where the code sits, so a chat name ahead of it costs no steps. */
    @Test
    public void staticRainbowIsAnchored() {
        assertEquals(TextEffectMath.computeStaticRainbowColor(0, 0), TextEffectMath.computeStaticRainbowColor(6, 6));
        assertNotEquals(TextEffectMath.computeStaticRainbowColor(6, 0), TextEffectMath.computeStaticRainbowColor(6, 6));
    }

    /** An escaped code renders as its own characters and styles nothing. */
    @Test
    public void escapedCodesAreDrawnAsText() {
        assertEquals("&z&q&v&u", RenderTextProcessor.prepare("\\&z\\&q\\&v\\&u", false).getDisplayText());
        assertEquals("&c literal", RenderTextProcessor.prepare("\\&c literal", false).getDisplayText());
    }

    /** The escape covers its own marker and nothing after it; real codes still convert. */
    @Test
    public void escapeDoesNotStopLaterCodes() {
        assertEquals("§cred &a not green §agreen",
            RenderTextProcessor.prepare("&cred \\&a not green &agreen", false).getDisplayText());
    }

    /**
     * A gradient token wears the two colours it names, and the ramp begins on the text
     * after it rather than on the code's own characters.
     */
    @Test
    public void gradientTokenWearsItsOwnColoursWhileEditing() {
        java.util.Map<Integer, java.util.List<kamkeel.hextext.api.rendering.RenderDirective>> directives =
            RenderTextProcessor.prepare("&g&#FFFF00&#FF00FFtext", true).getInstructions();

        assertTrue("start colour on the token", directives.containsKey(0));
        assertTrue("end colour on the second half", directives.containsKey(10));
        assertTrue("ramp begins after the token", directives.containsKey(18));
    }

    /** A backslash in front of anything else is just a backslash. */
    @Test
    public void loneBackslashIsUntouched() {
        assertEquals("§cpath\\to\\thing", RenderTextProcessor.prepare("&cpath\\to\\thing", false).getDisplayText());
    }
}
