package kamkeel.hextext.client.compat;

import com.gtnewhorizon.gtnhlib.util.font.FontRendering;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.common.util.ColorCodeUtils;

/**
 * HexText's answers to the questions GTNHLib asks about text. The string is returned
 * untouched - HexText reads its own codes and wants them intact - and the flags tell
 * the width code and Angelica's renderer which formatting rules apply.
 */
public final class HexTextPreprocessor implements FontRendering.TextPreprocessor {

    @Override
    public String apply(String text) {
        return text;
    }

    /** Live, since the config and the server's policy can both change it. */
    @Override
    public boolean handlesAmpCodes() {
        CommonProxy proxy = HexText.getActiveProxy();
        return proxy != null && proxy.allowUniversalAmpersand();
    }

    @Override
    public boolean hexColorResetsStyles() {
        return ColorCodeUtils.hexResetsStyles();
    }
}
