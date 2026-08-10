package kamkeel.hextext.client.compat;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.render.FontRenderContext;
import kamkeel.hextext.common.compat.AngelicaCompatibility;
import kamkeel.hextext.config.HexTextConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Covers the HexText -> Angelica grammar translation used when Angelica's batching font renderer
 * owns text drawing. The expected strings are Angelica's section-sign grammar: {@code §x} plus six
 * escaped nibbles for RGB, {@code §q} rainbow, {@code §z} wave, {@code §v} dinnerbone.
 */
public class AngelicaTextTranslatorTest {

    @Before
    public void setUp() {
        HexText.proxy = new CommonProxy();
        HexTextConfig.resetToDefaults();
        HexTextConfig.setUniversalAmpersandEnabled(true);
        HexTextConfig.setEnableRgbHtmlFormat(true);
        AngelicaClientCompat.setGlyphEffectsRegistered(false);
        AngelicaClientCompat.setHighlightRegistered(false);
        AngelicaClientCompat.setRainbowRegistered(false);
        AngelicaClientCompat.setAmpersandCodesConverted(Boolean.TRUE);
    }

    @After
    public void tearDown() {
        AngelicaClientCompat.setGlyphEffectsRegistered(false);
        AngelicaClientCompat.setHighlightRegistered(false);
        AngelicaClientCompat.setRainbowRegistered(false);
        AngelicaClientCompat.setAmpersandCodesConverted(null);
    }

    /** Angelica's §q never moves, so HexText's own rainbow is used where it registered. */
    @Test
    public void rainbowUsesHexTextsOwnEffectWhereItRegistered() {
        AngelicaClientCompat.setRainbowRegistered(true);
        assertEquals("§tRainbow", AngelicaTextTranslator.translate("&gRainbow"));
    }

    @Test
    public void translatesAmpersandHexToSectionX() {
        assertEquals("§x§f§f§0§0§0§0Hello", AngelicaTextTranslator.translate("&#FF0000Hello"));
    }

    @Test
    public void translatesSectionHexToSectionX() {
        assertEquals("§x§0§0§f§f§0§0Hi", AngelicaTextTranslator.translate("§#00FF00Hi"));
    }

    @Test
    public void nestedSpansRestoreEnclosingColor() {
        assertEquals(
            "§x§f§f§0§0§0§0red§x§0§0§f§f§0§0green§x§f§f§0§0§0§0back§rdone",
            AngelicaTextTranslator.translate("<FF0000>red<00FF00>green</00FF00>back</FF0000>done"));
    }

    @Test
    public void inlineHexResetsActiveStyles() {
        // APPLY_RGB carries resetFormatting in the native pipeline, so bold must not survive a hex colour.
        assertEquals(
            "§lBold§r§x§f§f§0§0§0§0Red",
            AngelicaTextTranslator.translate("&lBold&#FF0000Red"));
    }

    @Test
    public void spanOpenResetsActiveStyles() {
        assertEquals(
            "§lBold§r§x§f§f§0§0§0§0Red§rAfter",
            AngelicaTextTranslator.translate("&lBold<FF0000>Red</FF0000>After"));
    }

    @Test
    public void stylesSetInsideSpanSurvivePop() {
        // POP_COLOR is the one colour directive that keeps styles, so they are replayed after the reset.
        assertEquals(
            "§x§f§f§0§0§0§0§lRed§r§lAfter",
            AngelicaTextTranslator.translate("<FF0000>&lRed</FF0000>After"));
    }

    @Test
    public void spanPopRestoresVanillaColorAndReplaysStyles() {
        assertEquals(
            "§cVanilla§x§f§f§0§0§0§0Red§cBack",
            AngelicaTextTranslator.translate("&cVanilla<FF0000>Red</FF0000>Back"));
    }

    @Test
    public void resetInsideSpanClearsTheColorStack() {
        assertEquals(
            "§x§f§f§0§0§0§0a§rb§rc",
            AngelicaTextTranslator.translate("<FF0000>a&rb</FF0000>c"));
    }

    @Test
    public void bareRainbowBecomesAngelicaRainbow() {
        assertEquals("§qRainbow", AngelicaTextTranslator.translate("&gRainbow"));
    }

    @Test
    public void ampersandGradientFormIsExpandedPerGlyph() {
        // The gradient is not handed to Angelica: every glyph is coloured here with
        // HexText's own ramp, so a stock Angelica draws exactly what native draws.
        assertEquals(
            expandedGradient(0xFF0000, 0x0000FF, "Grad"),
            AngelicaTextTranslator.translate("&g&#FF0000&#0000FFGrad"));
    }

    private static String expandedGradient(int startRgb, int endRgb, String glyphs) {
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < glyphs.length(); i++) {
            expected.append(sectionX(kamkeel.hextext.common.util.TextEffectMath
                .computeGradientColor(startRgb, endRgb, i, glyphs.length())));
            expected.append(glyphs.charAt(i));
        }
        return expected.toString();
    }

    private static String sectionX(int rgb) {
        StringBuilder token = new StringBuilder("§x");
        for (int shift = 20; shift >= 0; shift -= 4) {
            token.append('§').append(Character.forDigit((rgb >> shift) & 0xF, 16));
        }
        return token.toString();
    }

    @Test
    public void effectsMapToAngelicaTogglesAndCloseBeforeColors() {
        assertEquals(
            "§vFlip§zShake§z§v§x§f§f§f§f§f§fWhite",
            AngelicaTextTranslator.translate("&hFlip&jShake&#FFFFFFWhite"));
    }

    @Test
    public void repeatedEffectCodesDoNotToggleOff() {
        assertEquals("§vFlip", AngelicaTextTranslator.translate("&h&hFlip"));
    }

    @Test
    public void igniteIsDropped() {
        assertEquals("Ignite", AngelicaTextTranslator.translate("&iIgnite"));
    }

    @Test
    public void vanillaColorCancelsActiveEffects() {
        assertEquals("§zWave§z§cRed", AngelicaTextTranslator.translate("&jWave&cRed"));
    }

    @Test
    public void resetCancelsEffectsWithoutExtraToggles() {
        assertEquals("§zWave§rPlain", AngelicaTextTranslator.translate("&jWave&rPlain"));
    }

    @Test
    public void translationIsIdempotent() {
        String once = AngelicaTextTranslator.translate("&#123456mixed<654321>span</654321>&gend");
        assertEquals(once, AngelicaTextTranslator.translate(new String(once.toCharArray())));
    }

    @Test
    public void angelicaTokensPassThroughUntouched() {
        assertEquals("§x§f§f§0§0§0§0Hi§qA§zB",
            AngelicaTextTranslator.translate("§x§f§f§0§0§0§0Hi§qA§zB"));
        assertEquals("§u§x§1§2§3§4§5§6shadow",
            AngelicaTextTranslator.translate("§u§x§1§2§3§4§5§6shadow"));
    }

    /** Even Angelica's own gradient spelling is expanded, so the ramp is HexText's. */
    @Test
    public void sectionGradientFormIsExpandedPerGlyph() {
        assertEquals(expandedGradient(0xFF0000, 0x0000FF, "gradient"),
            AngelicaTextTranslator.translate("§g§x§f§f§0§0§0§0§x§0§0§0§0§f§fgradient"));
    }

    @Test
    public void invalidTokensStayLiteral() {
        assertEquals("&#GGGGGG not hex", AngelicaTextTranslator.translate("&#GGGGGG not hex"));
        assertEquals("<FF00> too short", AngelicaTextTranslator.translate("<FF00> too short"));
        assertEquals("& lone ampersand", AngelicaTextTranslator.translate("& lone ampersand"));
    }

    @Test
    public void plainTextIsReturnedAsSameInstance() {
        String plain = "no formatting here";
        assertSame(plain, AngelicaTextTranslator.translate(plain));
    }

    @Test
    public void nullAndEmptyAreReturnedUnchanged() {
        assertNull(AngelicaTextTranslator.translate(null));
        assertEquals("", AngelicaTextTranslator.translate(""));
    }

    @Test
    public void ampersandTokensAreLiteralWhenUniversalAmpersandDisabled() {
        HexTextConfig.setUniversalAmpersandEnabled(false);
        assertEquals("&#FF0000literal &gliteral", AngelicaTextTranslator.translate("&#FF0000literal &gliteral"));
        // Section-sign forms are HexText syntax regardless of the ampersand gate.
        assertEquals("§x§f§f§0§0§0§0still works", AngelicaTextTranslator.translate("§#FF0000still works"));
    }

    @Test
    public void spanTokensAreLiteralWhenHtmlFormatDisabled() {
        HexTextConfig.setEnableRgbHtmlFormat(false);
        assertEquals("<FF0000>literal</FF0000>", AngelicaTextTranslator.translate("<FF0000>literal</FF0000>"));
    }

    /**
     * Raw is what the chat line draws while it is being typed, and it has to say two
     * things at once: here are the characters you typed, and here is what they do.
     * Each token is therefore emitted as its own literal spelling followed by the
     * section-sign token that colours the rest. Showing only the literal - which is
     * what this did before - is a code with no colour, and showing only the token is
     * a colour with no way to edit it.
     */
    @Test
    public void rawModeShowsTheCodeAndAppliesIt() {
        FontRenderContext.pushRawTextRendering();
        try {
            // Colour tokens wear their own colour ahead of the literal characters.
            assertEquals("§c&c§cHi §x§f§f§0§0§0§0&#FF0000§x§f§f§0§0§0§0there",
                AngelicaTextTranslator.translate("§cHi &#FF0000there"));
        } finally {
            FontRenderContext.popRawTextRendering();
        }
    }

    /**
     * The configuration this actually shipped under, and the one every other test here
     * turns off. Universal ampersand formatting governs text the world has already
     * produced; it has no bearing on a line being typed, which is on its way to a
     * converter that will make it a section sign the moment it is sent. Reading the
     * editor's environment as the world's is what left the chat line ignoring every
     * &-form code while the same string arrived coloured in the history above it.
     */
    @Test
    public void rawModeReadsAmpersandsEvenWhereTheWorldWouldNot() {
        HexTextConfig.setUniversalAmpersandEnabled(false);
        FontRenderContext.pushRawTextRendering();
        try {
            assertEquals("§c&c§cHi", AngelicaTextTranslator.translate("&cHi"));
            assertEquals("§x§1§2§3§1§2§3&#123123§x§1§2§3§1§2§3OKO",
                AngelicaTextTranslator.translate("&#123123OKO"));
        } finally {
            FontRenderContext.popRawTextRendering();
        }
    }

    /** Outside an editor the config still decides, and it says these are text. */
    @Test
    public void styledModeStillHonoursTheAmpersandGate() {
        HexTextConfig.setUniversalAmpersandEnabled(false);
        assertEquals("&cHi", AngelicaTextTranslator.translate("&cHi"));
    }

    /**
     * A gradient keeps its meaning through the conversion that sending performs.
     *
     * <p>{@code &g&#..&#..} is what gets typed; the chat converter makes every
     * ampersand a section sign on the way out, so the same gradient reaches the
     * history as {@code §g§#..§#..}. Matching only the typed spelling meant a
     * gradient previewed correctly and then arrived as flat rainbow with two
     * colours stacked after it - which reads as the whole line being the second
     * colour.</p>
     */
    @Test
    public void aGradientSurvivesBeingSent() {
        String expected = expandedGradient(0x4287f5, 0xe942f5, "HELLO");
        assertEquals(expected, AngelicaTextTranslator.translate("&g&#4287f5&#e942f5HELLO"));
        assertEquals(expected, AngelicaTextTranslator.translate("§g§#4287f5§#e942f5HELLO"));
        assertEquals(expected, AngelicaTextTranslator.translate("&g§#4287f5&#e942f5HELLO"));
    }

    /**
     * Angelica's own letters are spelled out at send: they are not HexText codes,
     * so the ordinary conversion leaves them - and on a server that has not allowed
     * ampersand formatting they arrive dead. The section form is read everywhere.
     */
    @Test
    public void angelicaSendCodesAreSpelledOut() {
        assertEquals("§qRAINBOW", AngelicaTextTranslator.convertAngelicaSendCodes("&qRAINBOW"));
        assertEquals("§vflip§v normal", AngelicaTextTranslator.convertAngelicaSendCodes("&vflip&v normal"));
        assertEquals("\\&q stays", AngelicaTextTranslator.convertAngelicaSendCodes("\\&q stays"));
        assertEquals("ends with §q", AngelicaTextTranslator.convertAngelicaSendCodes("ends with &q"));
        assertEquals("no codes here", AngelicaTextTranslator.convertAngelicaSendCodes("no codes here"));
    }

    /** The ramp spends itself on the glyphs it owns, not on text past its end. */
    @Test
    public void gradientSpanStopsAtItsTerminator() {
        assertEquals(expandedGradient(0xFF0000, 0x0000FF, "ramp ") + "§cthen",
            AngelicaTextTranslator.translate("&g&#FF0000&#0000FFramp &cthen"));
    }

    /**
     * Angelica carries styles through a colour where HexText clears them. Following its
     * rules drops the reset, so bold survives a hex the way it does for its own codes.
     */
    @Test
    public void angelicaRulesKeepStylesThroughAColour() {
        HexTextConfig.setFollowAngelicaFormatting(true);
        AngelicaCompatibility.setAngelicaFontRendererActive(true);
        try {
            assertEquals("§lBold§x§f§f§0§0§0§0Red", AngelicaTextTranslator.translate("&lBold&#FF0000Red"));
        } finally {
            AngelicaCompatibility.setAngelicaFontRendererActive(null);
        }
    }

    /** Turned off, HexText's own rules apply and a colour clears what came before. */
    @Test
    public void hexTextRulesClearStylesAtAColour() {
        HexTextConfig.setFollowAngelicaFormatting(false);
        AngelicaCompatibility.setAngelicaFontRendererActive(true);
        try {
            assertEquals("§lBold§r§x§f§f§0§0§0§0Red", AngelicaTextTranslator.translate("&lBold&#FF0000Red"));
        } finally {
            AngelicaCompatibility.setAngelicaFontRendererActive(null);
        }
    }

    /** Without two colours after it, g is still plain rainbow. */
    @Test
    public void gWithoutTwoColoursIsStillRainbow() {
        assertEquals("§qHELLO", AngelicaTextTranslator.translate("&gHELLO"));
        assertEquals("§q§x§4§2§8§7§f§5HELLO",
            AngelicaTextTranslator.translate("&g&#4287f5HELLO"));
    }

    /**
     * Angelica's own effects, typed with an ampersand.
     *
     * <p>Outside an editor Angelica converts these itself, so they always worked once
     * sent. Inside one the suppressor holds that conversion off - which is the whole
     * point, since the code has to stay visible - and HexText has to be the one that
     * knows what {@code &u}, {@code &q}, {@code &z} and {@code &v} mean. Until it did,
     * wave and drop shadow simply had no preview.</p>
     */
    @Test
    public void angelicaOwnEffectsAreReadInSectionForm() {
        assertEquals("§qRainbow", AngelicaTextTranslator.translate("§qRainbow"));
        assertEquals("§zWave", AngelicaTextTranslator.translate("§zWave"));
        assertEquals("§vFlip", AngelicaTextTranslator.translate("§vFlip"));
    }

    /**
     * Outside an editor the ampersand form is left alone, because it is not HexText's
     * to read. Angelica converts it downstream and HexText's own renderer draws those
     * characters as text, so consuming them here would make the translated path and
     * the native one disagree about a string neither mod owns. The fuzzed parity test
     * catches exactly this, and did.
     */
    @Test
    public void styledModeLeavesAngelicaAmpersandCodesAsText() {
        // q and v are Angelica's spellings of codes HexText writes as g and h, so
        // outside an editor they are somebody else's to convert.
        assertEquals("&qRainbow", AngelicaTextTranslator.translate("&qRainbow"));
        assertEquals("&vFlip", AngelicaTextTranslator.translate("&vFlip"));
    }

    /**
     * And the editor only previews them while Angelica is going to convert them. With
     * the setting off the code stays text once sent, so a preview that styled anyway
     * was showing the writer a line they were not going to get.
     */
    @Test
    public void angelicaAmpersandCodesOnlyPreviewWhereTheyWillConvert() {
        AngelicaClientCompat.setAmpersandCodesConverted(Boolean.FALSE);
        FontRenderContext.pushRawTextRendering();
        try {
            assertEquals("&qRainbow", AngelicaTextTranslator.translate("&qRainbow"));
            assertEquals("&vFlip", AngelicaTextTranslator.translate("&vFlip"));
        } finally {
            FontRenderContext.popRawTextRendering();
        }
    }

    /**
     * A backslash spoken for the marker behind it. Angelica reads the pair and draws a
     * bare ampersand, so the translator hands both characters on untouched; reading the
     * code here ate the ampersand and left the slash in front of a colour that was
     * never meant to apply.
     */
    @Test
    public void escapedCodesArePassedToAngelicaIntact() {
        assertEquals("\\&c literal", AngelicaTextTranslator.translate("\\&c literal"));
        assertEquals("\\&#FF0000literal hex", AngelicaTextTranslator.translate("\\&#FF0000literal hex"));
        assertEquals("\\&q\\&z\\&v\\&u", AngelicaTextTranslator.translate("\\&q\\&z\\&v\\&u"));
    }

    /** An escape covers its own token and nothing after it. */
    @Test
    public void escapeDoesNotStopLaterCodesTranslating() {
        assertEquals("§cred \\&a not green §agreen",
            AngelicaTextTranslator.translate("&cred \\&a not green &agreen"));
    }

    /** The section form escapes too, so a converted string can still be shown literally. */
    @Test
    public void escapedSectionSignIsPassedOnIntact() {
        assertEquals("§cred \\§aliteral", AngelicaTextTranslator.translate("&cred \\§aliteral"));
    }

    /** A backslash in front of anything else is just a backslash. */
    @Test
    public void loneBackslashIsUntouched() {
        assertEquals("§cpath\\to\\thing", AngelicaTextTranslator.translate("&cpath\\to\\thing"));
        assertEquals("a\\b", AngelicaTextTranslator.translate("a\\b"));
    }

    /** Wave and the shadow tint are HexText's own, so both renderers read them alike. */
    @Test
    public void hexTextsOwnCodesAreReadEverywhere() {
        assertEquals("§zWave", AngelicaTextTranslator.translate("&zWave"));
        assertEquals("§uShadow", AngelicaTextTranslator.translate("&uShadow"));
    }

    /** And they preview, which is what raw mode is for. */
    @Test
    public void angelicaOwnEffectsPreviewWhileBeingTyped() {
        HexTextConfig.setUniversalAmpersandEnabled(false);
        FontRenderContext.pushRawTextRendering();
        try {
            assertEquals("&z§zWave", AngelicaTextTranslator.translate("&zWave"));
            assertEquals("&u§u§x§e§9§4§2§f§5&#e942f5§x§e§9§4§2§f§5T",
                AngelicaTextTranslator.translate("&u&#e942f5T"));
        } finally {
            FontRenderContext.popRawTextRendering();
        }
    }

    /**
     * The wash that marks a code as a code while it is being edited.
     *
     * <p>Toggled on before the literal characters and off after them, so it covers the
     * token and nothing else. It has to be a toggle: a latched effect could only end by
     * resetting the colour standing in front of it, which is the colour the token was
     * just asked to set.</p>
     */
    @Test
    public void rawModeWashesTheTokenWhenAngelicaTookTheCode() {
        AngelicaClientCompat.setHighlightRegistered(true);
        FontRenderContext.pushRawTextRendering();
        try {
            assertEquals("§c§y&c§y§cHi", AngelicaTextTranslator.translate("&cHi"));
            assertEquals("§x§f§f§0§0§0§0§y&#FF0000§y§x§f§f§0§0§0§0T",
                AngelicaTextTranslator.translate("&#FF0000T"));
        } finally {
            FontRenderContext.popRawTextRendering();
        }
    }

    /** Refused the code, the characters still read - only the wash is missing. */
    @Test
    public void rawModeStillShowsTheCodeWithoutTheWash() {
        AngelicaClientCompat.setHighlightRegistered(false);
        FontRenderContext.pushRawTextRendering();
        try {
            assertEquals("§c&c§cHi", AngelicaTextTranslator.translate("&cHi"));
        } finally {
            FontRenderContext.popRawTextRendering();
        }
    }

    /** Nothing is washed outside an editor; the text there is not being written. */
    @Test
    public void styledModeNeverWashes() {
        AngelicaClientCompat.setHighlightRegistered(true);
        assertEquals("§cHi", AngelicaTextTranslator.translate("&cHi"));
    }

    /** The literal is always ampersands, or Angelica eats the thing being displayed. */
    @Test
    public void rawModeSpellsSectionTokensWithAmpersands() {
        FontRenderContext.pushRawTextRendering();
        try {
            // A style token previews itself, so its own characters are bold too.
            assertEquals("§l&l§lBold", AngelicaTextTranslator.translate("§lBold"));
        } finally {
            FontRenderContext.popRawTextRendering();
        }
    }

    /** An ampersand that spells nothing is text, and text is left alone. */
    @Test
    public void rawModeLeavesLooseAmpersandsAsThemselves() {
        FontRenderContext.pushRawTextRendering();
        try {
            assertEquals("me & you", AngelicaTextTranslator.translate("me & you"));
            assertEquals("50& off", AngelicaTextTranslator.translate("50& off"));
        } finally {
            FontRenderContext.popRawTextRendering();
        }
    }

    /**
     * The literal must be exactly what was consumed, for every form there is. A short
     * literal drops characters out from under the cursor and a long one doubles them,
     * and either way the text stops matching what the reader typed.
     */
    @Test
    public void rawModeReproducesEveryTokenExactly() {
        AngelicaClientCompat.setGlyphEffectsRegistered(true);
        FontRenderContext.pushRawTextRendering();
        try {
            for (String token : new String[] {
                "&c", "&l", "&r", "&#FF0000", "§#00FF00", "&g", "&h", "&i", "&j",
                "§x§f§f§0§0§0§0", "§q", "§z", "§v",
                "&g&#FF0000&#00FF00", "<FF0000>", "</FF0000>",
            }) {
                String raw = AngelicaTextTranslator.translate(token + "T");
                String expectedLiteral = token.replace('§', '&');
                // Tokens are interleaved with the colours they preview, so the literal
                // is checked against what is left once those are taken out.
                String characters = raw.replaceAll("§.", "");
                assertEquals(token + " -> " + raw, expectedLiteral + "T", characters);
            }
        } finally {
            FontRenderContext.popRawTextRendering();
            AngelicaClientCompat.setGlyphEffectsRegistered(false);
        }
    }

    /** Raw and styled share one cache slot, so the key has to tell them apart. */
    @Test
    public void rawAndStyledDoNotShareACacheEntry() {
        String text = "&cHi";
        assertEquals("§cHi", AngelicaTextTranslator.translate(text));
        FontRenderContext.pushRawTextRendering();
        try {
            assertEquals("§c&c§cHi", AngelicaTextTranslator.translate(text));
        } finally {
            FontRenderContext.popRawTextRendering();
        }
        assertEquals("§cHi", AngelicaTextTranslator.translate(text));
    }

    @Test
    public void sectionEffectCodesTranslateWithoutTheAmpersandGate() {
        HexTextConfig.setUniversalAmpersandEnabled(false);
        assertEquals("§qRainbow §vFlip", AngelicaTextTranslator.translate("§gRainbow §hFlip"));
    }

    @Test
    public void igniteUsesTheEffectRegistryWhenAvailable() {
        AngelicaClientCompat.setGlyphEffectsRegistered(true);
        assertEquals("§iIgnite", AngelicaTextTranslator.translate("&iIgnite"));
        assertEquals("§iIgnite", AngelicaTextTranslator.translate("§iIgnite"));
    }

    @Test
    public void shakeUsesTheEffectRegistryWhenAvailable() {
        AngelicaClientCompat.setGlyphEffectsRegistered(true);
        assertEquals("§jShake", AngelicaTextTranslator.translate("&jShake"));
        // Angelica clears registry effects on colour codes itself, so no closing toggle is needed.
        assertEquals("§jWave§cRed", AngelicaTextTranslator.translate("&jWave&cRed"));
    }

    @Test
    public void registryModeIsIdempotent() {
        AngelicaClientCompat.setGlyphEffectsRegistered(true);
        String once = AngelicaTextTranslator.translate("&iburn &jjolt &#FF0000red");
        assertEquals(once, AngelicaTextTranslator.translate(new String(once.toCharArray())));
    }

    @Test
    public void cacheDistinguishesRegistryAvailability() {
        String input = "&iIgnite";
        assertEquals("Ignite", AngelicaTextTranslator.translate(input));
        AngelicaClientCompat.setGlyphEffectsRegistered(true);
        assertEquals("§iIgnite", AngelicaTextTranslator.translate(input));
    }
}
