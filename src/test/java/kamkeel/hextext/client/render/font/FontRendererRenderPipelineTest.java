package kamkeel.hextext.client.render.font;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.config.HexTextConfig;
import kamkeel.hextext.common.util.ColorCodeUtils;
import net.minecraft.client.gui.FontRenderer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FontRendererRenderPipelineTest {

    @Before
    public void setUp() {
        HexText.proxy = new CommonProxy();
        HexTextConfig.resetToDefaults();
        HexTextConfig.setUniversalAmpersandEnabled(true);
        HexTextConfig.setEnableRgbHtmlFormat(true);
    }

    @Test
    public void testBoldPersistsWhenEffectsFollow() {
        StubBridge bridge = new StubBridge();
        FontRendererRenderPipeline pipeline = new FontRendererRenderPipeline(bridge);

        String original = "&g&l&iTesting";
        pipeline.begin(original, false);
        String sanitized = pipeline.adjustRenderText(original);
        assertEquals("\u00A7lTesting", sanitized);

        boolean skipNext = false;
        boolean observedGlyphBold = false;
        boolean glyphSeen = false;

        for (int i = 0; i < sanitized.length(); i++) {
            char current = sanitized.charAt(i);
            pipeline.applyInstructions(sanitized, i, current);

            if (skipNext) {
                skipNext = false;
                continue;
            }

            if (current == '\u00A7' && i + 1 < sanitized.length()) {
                char formatChar = Character.toLowerCase(sanitized.charAt(i + 1));
                if (ColorCodeUtils.isMinecraftColorCode(formatChar) || formatChar == 'g') {
                    bridge.hexText$resetFormattingStyles();
                } else if (ColorCodeUtils.isStyleCode(formatChar)) {
                    switch (formatChar) {
                        case 'k':
                            bridge.hexText$setRandomStyle(true);
                            break;
                        case 'l':
                            bridge.hexText$setBoldStyle(true);
                            break;
                        case 'm':
                            bridge.hexText$setStrikethroughStyle(true);
                            break;
                        case 'n':
                            bridge.hexText$setUnderlineStyle(true);
                            break;
                        case 'o':
                            bridge.hexText$setItalicStyle(true);
                            break;
                        default:
                            break;
                    }
                }
                skipNext = true;
                continue;
            }

            if (!glyphSeen && current != '\u00A7') {
                glyphSeen = true;
                observedGlyphBold = bridge.boldStyle;
            }
        }

        assertTrue("Bold style should remain enabled after ignite", observedGlyphBold);
    }

    private static final class StubBridge implements FontRendererBridge {

        private boolean randomStyle;
        private boolean boldStyle;
        private boolean strikethroughStyle;
        private boolean underlineStyle;
        private boolean italicStyle;
        private int textColor = 0xFFFFFF;
        private float alpha = 1.0f;
        private float red = 1.0f;
        private float green = 1.0f;
        private float blue = 1.0f;

        @Override
        public FontRenderer hexText$getFontRenderer() {
            return null;
        }

        @Override
        public void hexText$setRandomStyle(boolean enabled) {
            randomStyle = enabled;
        }

        @Override
        public void hexText$setBoldStyle(boolean enabled) {
            boldStyle = enabled;
        }

        @Override
        public void hexText$setStrikethroughStyle(boolean enabled) {
            strikethroughStyle = enabled;
        }

        @Override
        public void hexText$setUnderlineStyle(boolean enabled) {
            underlineStyle = enabled;
        }

        @Override
        public void hexText$setItalicStyle(boolean enabled) {
            italicStyle = enabled;
        }

        @Override
        public void hexText$setTextColor(int color) {
            textColor = color;
        }

        @Override
        public int hexText$getTextColor() {
            return textColor;
        }

        @Override
        public float hexText$getAlpha() {
            return alpha;
        }

        @Override
        public float hexText$getRedComponent() {
            return red;
        }

        @Override
        public float hexText$getBlueComponent() {
            return blue;
        }

        @Override
        public float hexText$getGreenComponent() {
            return green;
        }

        @Override
        public int[] hexText$getColorCodePalette() {
            return new int[32];
        }

        @Override
        public float hexText$getPosX() {
            return 0.0f;
        }

        @Override
        public float hexText$getPosY() {
            return 0.0f;
        }

        @Override
        public int hexText$getFontHeight() {
            return 9;
        }

        @Override
        public void hexText$applyColorComponents(float red, float green, float blue, float alpha) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }

        @Override
        public void hexText$resetFormattingStyles() {
            randomStyle = false;
            boldStyle = false;
            strikethroughStyle = false;
            underlineStyle = false;
            italicStyle = false;
        }
    }
}
