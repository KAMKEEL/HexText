package kamkeel.hextext.common.render;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.api.rendering.RenderDirective;
import kamkeel.hextext.api.rendering.RenderPlan;
import kamkeel.hextext.config.HexTextConfig;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Wave, gradient and section-x read by HexText's own pre-processor, with no Angelica
 * anywhere. These were Angelica-only until now: the compat layer translated HexText's
 * codes into Angelica's grammar, which meant the effects existed only while Angelica
 * was installed to render them.
 */
public class NativeEffectParsingTest {

    @Before
    public void setUp() {
        HexText.proxy = new CommonProxy();
        HexTextConfig.resetToDefaults();
        HexTextConfig.setUniversalAmpersandEnabled(true);
    }

    private static List<RenderDirective> directivesAt(String text, int index) {
        RenderPlan plan = RenderTextProcessor.prepare(text, false);
        Map<Integer, List<RenderDirective>> instructions = plan.getInstructions();
        assertNotNull("no directives at all for: " + text, instructions);
        return instructions.get(index);
    }

    @Test
    public void waveIsReadAsItsOwnDirective() {
        List<RenderDirective> at = directivesAt("&zWave", 0);
        assertNotNull("wave produced nothing", at);
        assertEquals(1, at.size());
        assertEquals(RenderDirectiveImpl.Type.SET_WAVE, at.get(0).getType());
        assertTrue(at.get(0).isEnabled());
    }

    @Test
    public void sectionXIsReadAsAColour() {
        List<RenderDirective> at = directivesAt("§x§1§2§3§1§2§3Teal", 0);
        assertNotNull("section-x produced nothing", at);
        assertEquals(RenderDirectiveImpl.Type.APPLY_RGB, at.get(0).getType());
        assertEquals(0x123123, at.get(0).getRgb());
    }

    /** Both colours, and a span that counts only the glyphs actually drawn. */
    @Test
    public void gradientCarriesBothColoursAndItsSpan() {
        List<RenderDirective> at = directivesAt("&g&#4287f5&#e942f5HELLO", 0);
        assertNotNull("gradient produced nothing", at);
        RenderDirective directive = at.get(0);
        assertEquals(RenderDirectiveImpl.Type.SET_GRADIENT, directive.getType());
        assertEquals(0x4287f5, directive.getRgb());
        assertEquals(0xe942f5, ((RenderDirectiveImpl) directive).getSecondaryRgb());
        assertEquals("HELLO".length(), directive.getParameter());
    }

    /** The section-sign spelling is the same gradient; it is what sending produces. */
    @Test
    public void gradientIsReadInEitherSpelling() {
        List<RenderDirective> at = directivesAt("§g§#4287f5§#e942f5HELLO", 0);
        assertNotNull("section gradient produced nothing", at);
        assertEquals(RenderDirectiveImpl.Type.SET_GRADIENT, at.get(0).getType());
        assertEquals(0x4287f5, at.get(0).getRgb());
    }

    /** Codes in the tail take no width, so they must not lengthen the ramp. */
    @Test
    public void theSpanCountsGlyphsAndNotCodes() {
        List<RenderDirective> at = directivesAt("&g&#000000&#ffffffAB&lCD", 0);
        assertEquals(4, at.get(0).getParameter());
    }

    /** &u carrying a colour tints the shadow the text already casts. */
    @Test
    public void shadowTintCarriesItsColour() {
        List<RenderDirective> at = directivesAt("&u&#e942f5Text", 0);
        assertNotNull("shadow tint produced nothing", at);
        assertEquals(RenderDirectiveImpl.Type.SET_SHADOW_COLOR, at.get(0).getType());
        assertEquals(0xe942f5, at.get(0).getRgb());
        assertTrue(at.get(0).isEnabled());
    }

    /** Written bare it clears the tint, so the darkened base colour returns. */
    @Test
    public void aBareShadowCodeClearsTheTint() {
        List<RenderDirective> at = directivesAt("&uText", 0);
        assertNotNull(at);
        assertEquals(RenderDirectiveImpl.Type.SET_SHADOW_COLOR, at.get(0).getType());
        assertFalse("bare &u must not set a colour", at.get(0).isEnabled());
    }

    /** The section spelling is the same code; it is what sending produces. */
    @Test
    public void shadowTintIsReadInEitherSpelling() {
        List<RenderDirective> at = directivesAt("§u§#e942f5Text", 0);
        assertNotNull(at);
        assertEquals(RenderDirectiveImpl.Type.SET_SHADOW_COLOR, at.get(0).getType());
        assertEquals(0xe942f5, at.get(0).getRgb());
    }

    /** A lone g is still rainbow; only two colours after it make a gradient. */
    @Test
    public void gWithoutTwoColoursIsStillRainbow() {
        List<RenderDirective> at = directivesAt("&gRainbow", 0);
        assertNotNull(at);
        assertEquals(RenderDirectiveImpl.Type.SET_RAINBOW, at.get(0).getType());
    }
}
