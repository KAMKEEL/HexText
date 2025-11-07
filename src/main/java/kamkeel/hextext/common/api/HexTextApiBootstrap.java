package kamkeel.hextext.common.api;

import kamkeel.hextext.HexText;
import kamkeel.hextext.api.HexTextApi;
import kamkeel.hextext.api.HexTextApiProvider;
import kamkeel.hextext.api.sign.IHexTextSign;
import kamkeel.hextext.api.sign.SignInteractionRegistry;
import kamkeel.hextext.api.sign.SignSide;
import kamkeel.hextext.api.sign.SignStateApi;
import kamkeel.hextext.api.text.SignTextApi;
import kamkeel.hextext.api.text.TextFormattingApi;
import kamkeel.hextext.common.sign.HexTextSignInteractions;
import kamkeel.hextext.common.sign.SignSideHelper;
import kamkeel.hextext.common.util.ColorCodeUtils;
import kamkeel.hextext.common.util.SignTextHelper;
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
        private final SignTextApi signText = new SignTextApiImpl();
        private final TextFormattingApi textFormatting = new TextFormattingApiImpl();
        private final SignStateApi signState = new SignStateApiImpl();

        @Override
        public String modVersion() {
            return HexText.VERSION;
        }

        @Override
        public SignInteractionRegistry signInteractions() {
            return signInteractions;
        }

        @Override
        public SignTextApi signText() {
            return signText;
        }

        @Override
        public TextFormattingApi textFormatting() {
            return textFormatting;
        }

        @Override
        public SignStateApi signState() {
            return signState;
        }

        private final class SignTextApiImpl implements SignTextApi {

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

        private final class TextFormattingApiImpl implements TextFormattingApi {

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

        private final class SignStateApiImpl implements SignStateApi {

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
