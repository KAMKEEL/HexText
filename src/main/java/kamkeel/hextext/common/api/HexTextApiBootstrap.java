package kamkeel.hextext.common.api;

import kamkeel.hextext.HexText;
import kamkeel.hextext.api.HexTextApi;
import kamkeel.hextext.api.HexTextApiProvider;
import kamkeel.hextext.api.rendering.ColorService;
import kamkeel.hextext.api.rendering.DynamicEffectService;
import kamkeel.hextext.api.rendering.RenderPlan;
import kamkeel.hextext.api.rendering.RenderingEnvironmentService;
import kamkeel.hextext.api.rendering.TextRenderService;
import kamkeel.hextext.api.rendering.TokenHighlightService;
import kamkeel.hextext.api.rendering.HighlightSpan;
import kamkeel.hextext.api.sign.IHexTextSign;
import kamkeel.hextext.api.sign.SignInteractionRegistry;
import kamkeel.hextext.api.sign.SignSide;
import kamkeel.hextext.api.sign.SignStateService;
import kamkeel.hextext.api.text.SignTextService;
import kamkeel.hextext.api.text.TextFormatter;
import kamkeel.hextext.api.text.TextSanitizer;
import kamkeel.hextext.common.render.HighlightComputations;
import kamkeel.hextext.common.render.HighlightSpanImpl;
import kamkeel.hextext.common.render.RenderTextProcessor;
import kamkeel.hextext.common.sign.HexTextSignInteractions;
import kamkeel.hextext.common.sign.SignSideHelper;
import kamkeel.hextext.common.util.ColorCodeUtils;
import kamkeel.hextext.common.util.ColorMath;
import kamkeel.hextext.common.util.SignTextHelper;
import kamkeel.hextext.common.util.StringUtils;
import kamkeel.hextext.common.util.TextEffectMath;
import kamkeel.hextext.config.HexTextConfig;
import kamkeel.hextext.client.render.FontRenderContext;
import net.minecraft.tileentity.TileEntitySign;

import java.util.Arrays;

/**
 * Bootstraps the HexText API provider with the concrete implementations used by the mod.
 */
public final class HexTextApiBootstrap {

    private static boolean installed;

    private HexTextApiBootstrap() {
    }

    public static void initialize() {
        if (installed) {
            return;
        }
        HexTextApi.installProvider(new Provider());
        installed = true;
    }

    private static final class Provider implements HexTextApiProvider {

        private final SignInteractionRegistry signInteractions = new HexTextSignInteractions();
        private final SignTextService signText = new SignTextServiceImpl();
        private final TextFormatter textFormatter = new TextFormatterImpl();
        private final TextSanitizer textSanitizer = new TextSanitizerImpl();
        private final TextRenderService textRenderer = new TextRenderServiceImpl();
        private final TokenHighlightService tokenHighlighter = new TokenHighlightServiceImpl();
        private final SignStateService signState = new SignStateServiceImpl();
        private final RenderingEnvironmentService renderEnvironment = new RenderingEnvironmentServiceImpl();
        private final DynamicEffectService dynamicEffects = new DynamicEffectServiceImpl();
        private final ColorService colors = new ColorServiceImpl();

        @Override
        public String modVersion() {
            return HexText.VERSION;
        }

        @Override
        public SignInteractionRegistry signInteractions() {
            return signInteractions;
        }

        @Override
        public SignTextService signText() {
            return signText;
        }

        @Override
        public TextFormatter textFormatter() {
            return textFormatter;
        }

        @Override
        public TextSanitizer textSanitizer() {
            return textSanitizer;
        }

        @Override
        public TextRenderService textRenderer() {
            return textRenderer;
        }

        @Override
        public TokenHighlightService tokenHighlighter() {
            return tokenHighlighter;
        }

        @Override
        public SignStateService signState() {
            return signState;
        }

        @Override
        public RenderingEnvironmentService renderEnvironment() {
            return renderEnvironment;
        }

        @Override
        public DynamicEffectService dynamicEffects() {
            return dynamicEffects;
        }

        @Override
        public ColorService colors() {
            return colors;
        }

        private final class RenderingEnvironmentServiceImpl implements RenderingEnvironmentService {

            @Override
            public boolean isRawTextRendering() {
                return FontRenderContext.isRawTextRendering();
            }

            @Override
            public void pushRawTextRendering() {
                FontRenderContext.pushRawTextRendering();
            }

            @Override
            public void popRawTextRendering() {
                FontRenderContext.popRawTextRendering();
            }
        }

        private final class DynamicEffectServiceImpl implements DynamicEffectService {

            private static final float RAINBOW_SPREAD_DEGREES = 12.0f;
            private static final float IGNITE_MIN_FACTOR = 0.35f;
            private static final float SHAKE_VERTICAL_RANGE = 1.05f;

            @Override
            public int computeRainbowColor(long now, int glyphIndex, int anchorIndex) {
                return TextEffectMath.computeRainbowColor(
                    now,
                    HexTextConfig.getRainbowSpeed(),
                    glyphIndex,
                    anchorIndex,
                    RAINBOW_SPREAD_DEGREES
                );
            }

            @Override
            public int computeIgniteColor(long now, int baseColor) {
                float brightness = TextEffectMath.computeIgniteBrightness(
                    now,
                    HexTextConfig.getIgniteInterval(),
                    IGNITE_MIN_FACTOR
                );
                return ColorMath.scaleBrightness(baseColor, brightness);
            }

            @Override
            public float computeShakeOffset(long now, int glyphIndex) {
                long seed = TextEffectMath.computeShakeSeed(
                    glyphIndex,
                    now,
                    HexTextConfig.getShakeInterval()
                );
                return TextEffectMath.computeShakeOffset(seed, SHAKE_VERTICAL_RANGE);
            }
        }

        private final class ColorServiceImpl implements ColorService {

            @Override
            public int calculateShadowColor(int rgb) {
                return ColorCodeUtils.calculateShadowColor(rgb);
            }
        }

        private final class SignTextServiceImpl implements SignTextService {

            @Override
            public int visibleCharacterLimit() {
                return SignTextHelper.SIGN_LINE_VISIBLE_LIMIT;
            }

            @Override
            public int visibleLength(CharSequence text) {
                return SignTextHelper.visibleLength(text);
            }

            @Override
            public String clampToVisibleLimit(String text) {
                return SignTextHelper.clampToVisibleLimit(text);
            }

            @Override
            public void copyText(String[] src, String[] dst) {
                SignTextHelper.copyText(src, dst);
            }

            @Override
            public void copyTextClamped(String[] src, String[] dst) {
                SignTextHelper.copyTextClamped(src, dst);
            }

            @Override
            public void copyTextSanitizedClamped(String[] src, String[] dst) {
                SignTextHelper.copyTextSanitizedClamped(src, dst);
            }

            @Override
            public String filterAllowedCharacters(String input) {
                return SignTextHelper.signedAllowCharacters(input);
            }
        }

        private final class TextFormatterImpl implements TextFormatter {

            @Override
            public FormattingEnvironment captureEnvironment(boolean rawMode) {
                return new FormattingEnvironmentAdapter(ColorCodeUtils.captureFormattingEnvironment(rawMode));
            }

            @Override
            public int detectColorCodeLength(CharSequence text, int index, boolean raw, FormattingEnvironment environment) {
                if (environment == null) {
                    return ColorCodeUtils.detectColorCodeLength(text, index, raw);
                }
                ColorCodeUtils.FormattingEnvironment delegate = unwrap(environment);
                if (delegate != null) {
                    return ColorCodeUtils.detectColorCodeLength(text, index, raw, delegate);
                }
                return ColorCodeUtils.detectColorCodeLength(
                    text,
                    index,
                    raw,
                    environment.allowsHtmlFormatting(),
                    environment.allowsUniversalAmpersand()
                );
            }

            @Override
            public int detectColorCodeLength(CharSequence text, int index) {
                return ColorCodeUtils.detectColorCodeLength(text, index);
            }

            @Override
            public boolean containsFormattingCodes(CharSequence text) {
                return ColorCodeUtils.containsFormattingCodes(text);
            }

            @Override
            public int indexOfNextFormattingCode(CharSequence text, int start) {
                return ColorCodeUtils.indexOfNextFormattingCode(text, start);
            }

            @Override
            public boolean isValidHexString(String hex) {
                return ColorCodeUtils.isValidHexString(hex);
            }

            @Override
            public int parseHexColor(String hex) {
                return ColorCodeUtils.parseHexColor(hex);
            }

            @Override
            public int hsvToRgb(float hue, float saturation, float value) {
                return ColorCodeUtils.hsvToRgb(hue, saturation, value);
            }

            private ColorCodeUtils.FormattingEnvironment unwrap(FormattingEnvironment environment) {
                if (environment instanceof FormattingEnvironmentAdapter) {
                    return ((FormattingEnvironmentAdapter) environment).delegate;
                }
                return null;
            }

            private final class FormattingEnvironmentAdapter implements FormattingEnvironment {

                private final ColorCodeUtils.FormattingEnvironment delegate;

                private FormattingEnvironmentAdapter(ColorCodeUtils.FormattingEnvironment delegate) {
                    this.delegate = delegate;
                }

                @Override
                public boolean allowsHtmlFormatting() {
                    return delegate != null && delegate.allowsHtmlFormatting();
                }

                @Override
                public boolean allowsUniversalAmpersand() {
                    return delegate != null && delegate.allowsUniversalAmpersand();
                }
            }
        }

        private final class TextSanitizerImpl implements TextSanitizer {

            @Override
            public String normalizeForRawDisplay(String text) {
                return StringUtils.normalizeForRawDisplay(text);
            }

            @Override
            public String convertAmpersandsToSectionSigns(String text) {
                return StringUtils.convertAmpersandsToSectionSigns(text);
            }

            @Override
            public String convertSectionSignsToAmpersands(String text) {
                return StringUtils.convertSectionSignsToAmpersands(text);
            }

            @Override
            public String stripColorCodes(CharSequence input) {
                return StringUtils.stripColorCodes(input);
            }

            @Override
            public String stripExtras(CharSequence input) {
                return StringUtils.stripExtras(input);
            }

            @Override
            public String stripHexColors(CharSequence input) {
                return StringUtils.stripHexColors(input);
            }

            @Override
            public String stripColors(CharSequence input) {
                return StringUtils.stripColors(input);
            }

            @Override
            public String stripStyles(CharSequence input) {
                return StringUtils.stripStyles(input);
            }

            @Override
            public boolean containsFormattingCodes(CharSequence input) {
                return StringUtils.containsFormattingCodes(input);
            }
        }

        private final class TextRenderServiceImpl implements TextRenderService {

            @Override
            public RenderPlan prepare(String text, boolean rawMode) {
                return RenderTextProcessor.prepare(text, rawMode);
            }
        }

        private final class TokenHighlightServiceImpl implements TokenHighlightService {

            @Override
            public float measureLiteralWidth(WidthProvider provider, CharSequence text, int start, int length) {
                return HighlightComputations.measureLiteralWidth(provider, text, start, length);
            }

            @Override
            public int getTokenHighlightColor(CharSequence text, int index) {
                return HighlightComputations.getTokenHighlightColor(text, index);
            }

            @Override
            public HighlightSpan createHighlight(float x, float y, float width, int color) {
                return new HighlightSpanImpl(x, y, width, color);
            }
        }

        private final class SignStateServiceImpl implements SignStateService {

            @Override
            public boolean isHexTextSign(TileEntitySign sign) {
                return sign instanceof IHexTextSign;
            }

            @Override
            public boolean isWaxed(TileEntitySign sign) {
                if (!(sign instanceof IHexTextSign)) {
                    return false;
                }
                return ((IHexTextSign) sign).isWaxed();
            }

            @Override
            public void setWaxed(TileEntitySign sign, boolean waxed) {
                if (sign instanceof IHexTextSign) {
                    ((IHexTextSign) sign).setWaxed(waxed);
                }
            }

            @Override
            public boolean isGlowing(TileEntitySign sign, SignSide side) {
                if (!(sign instanceof IHexTextSign) || side == null) {
                    return false;
                }
                return ((IHexTextSign) sign).isGlowing(side);
            }

            @Override
            public boolean setGlowing(TileEntitySign sign, SignSide side, boolean glowing) {
                if (!(sign instanceof IHexTextSign) || side == null) {
                    return false;
                }
                return ((IHexTextSign) sign).setGlowing(side, glowing);
            }

            @Override
            public boolean isOutlined(TileEntitySign sign, SignSide side) {
                if (!(sign instanceof IHexTextSign) || side == null) {
                    return false;
                }
                return ((IHexTextSign) sign).isOutlined(side);
            }

            @Override
            public boolean setOutlined(TileEntitySign sign, SignSide side, boolean outlined) {
                if (!(sign instanceof IHexTextSign) || side == null) {
                    return false;
                }
                return ((IHexTextSign) sign).setOutlined(side, outlined);
            }

            @Override
            public String[] getLines(TileEntitySign sign, SignSide side) {
                String[] copy = new String[4];
                if (!(sign instanceof IHexTextSign) || side == null) {
                    Arrays.fill(copy, "");
                    return copy;
                }
                String[] source = ((IHexTextSign) sign).getLines(side);
                SignTextHelper.copyText(source, copy);
                return copy;
            }

            @Override
            public void setEditingSide(TileEntitySign sign, SignSide side) {
                if (sign instanceof IHexTextSign && side != null) {
                    ((IHexTextSign) sign).setEditSide(side);
                }
            }

            @Override
            public SignSide getEditingSide(TileEntitySign sign) {
                if (sign instanceof IHexTextSign) {
                    return ((IHexTextSign) sign).getEditSide();
                }
                return SignSide.FRONT;
            }

            @Override
            public SignSide determineSide(TileEntitySign sign, double playerX, double playerZ, float hitX, float hitZ) {
                return SignSideHelper.determineSide(sign, playerX, playerZ, hitX, hitZ);
            }
        }
    }
}
