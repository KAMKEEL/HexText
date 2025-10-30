package kamkeel.hextext.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HexTextConfigTest {

    @Before
    public void setUp() {
        HexTextConfig.resetToDefaults();
    }

    @After
    public void tearDown() {
        HexTextConfig.resetToDefaults();
    }

    @Test
    public void defaultsReflectExpectedValues() {
        assertFalse(HexTextConfig.isRgbHtmlFormatEnabled());
        assertTrue(HexTextConfig.isSignEditingAllowed());
        assertTrue(HexTextConfig.isAmpersandAllowed());
    }

    @Test
    public void settersOverrideServerFlags() {
        HexTextConfig.setEnableRgbHtmlFormat(true);
        HexTextConfig.setAllowSignEditing(false);
        HexTextConfig.setAllowAmpersand(false);

        assertTrue(HexTextConfig.isRgbHtmlFormatEnabled());
        assertFalse(HexTextConfig.isSignEditingAllowed());
        assertFalse(HexTextConfig.isAmpersandAllowed());
    }
}
