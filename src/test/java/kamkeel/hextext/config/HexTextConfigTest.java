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
        assertFalse(HexTextConfig.isUniversalAmpersandEnabled());
        assertTrue(HexTextConfig.isChatAmpersandConversionEnabled());
        assertTrue(HexTextConfig.isSignAmpersandConversionEnabled());
        assertTrue(HexTextConfig.isRepairAmpersandConversionEnabled());
    }

    @Test
    public void settersOverrideServerFlags() {
        HexTextConfig.setEnableRgbHtmlFormat(true);
        HexTextConfig.setAllowSignEditing(false);
        HexTextConfig.setUniversalAmpersandEnabled(false);
        HexTextConfig.setChatAmpersandConversionEnabled(true);
        HexTextConfig.setSignAmpersandConversionEnabled(true);
        HexTextConfig.setRepairAmpersandConversionEnabled(true);

        assertTrue(HexTextConfig.isRgbHtmlFormatEnabled());
        assertFalse(HexTextConfig.isSignEditingAllowed());
        assertFalse(HexTextConfig.isUniversalAmpersandEnabled());
        assertTrue(HexTextConfig.isChatAmpersandConversionEnabled());
        assertTrue(HexTextConfig.isSignAmpersandConversionEnabled());
        assertTrue(HexTextConfig.isRepairAmpersandConversionEnabled());
    }
}
