package kamkeel.hextext.client.compat;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.render.FontRenderContext;
import kamkeel.hextext.config.HexTextConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Angelica's ampersand conversion is a client setting, and Angelica is a client mod, so
 * a server has no way to turn it off. Declining to convert is the one lever that works
 * from this side: HexText cannot change what a player has chosen, but it can stop acting
 * on it while that player is somewhere the codes are not allowed.
 */
public class AngelicaConversionPolicyTest {

    @Before
    public void setUp() {
        HexText.proxy = new CommonProxy();
        HexTextConfig.resetToDefaults();
    }

    @After
    public void tearDown() {
        while (FontRenderContext.isRawTextRendering()) {
            FontRenderContext.popRawTextRendering();
        }
        HexTextConfig.resetToDefaults();
    }

    /** Allowed, and not being edited: Angelica converts as it always did. */
    @Test
    public void conversionRunsWhereTheCodesAreAllowed() {
        HexTextConfig.setUniversalAmpersandEnabled(true);
        assertFalse(AngelicaClientCompat.shouldSuppressConversion());
    }

    /** Not allowed: the ampersands stay characters, without touching anyone's config. */
    @Test
    public void conversionIsHeldOffWhereTheCodesAreNot() {
        HexTextConfig.setUniversalAmpersandEnabled(false);
        assertTrue(AngelicaClientCompat.shouldSuppressConversion());
    }

    /** An editor suppresses regardless, or the code vanishes as it is being written. */
    @Test
    public void editingAlwaysSuppressesWhateverThePolicySays() {
        HexTextConfig.setUniversalAmpersandEnabled(true);
        FontRenderContext.pushRawTextRendering();
        try {
            assertTrue(AngelicaClientCompat.shouldSuppressConversion());
        } finally {
            FontRenderContext.popRawTextRendering();
        }
    }

    /** Before anything has connected there is no policy, so nothing is withheld. */
    @Test
    public void noProxyMeansNoPolicyToEnforce() {
        HexText.proxy = null;
        assertFalse(AngelicaClientCompat.shouldSuppressConversion());
    }
}
