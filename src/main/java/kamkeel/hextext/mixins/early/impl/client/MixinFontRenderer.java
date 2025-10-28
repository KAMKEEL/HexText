package kamkeel.hextext.mixins.early.impl.client;

import kamkeel.hextext.ColorCodeUtils;
import kamkeel.hextext.client.FormattedTextMetrics;
import kamkeel.hextext.client.LegacyFontRenderContext;
import kamkeel.hextext.client.LegacyRenderAction;
import kamkeel.hextext.client.LegacyRenderTextData;
import kamkeel.hextext.client.TokenHighlight;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = FontRenderer.class)
public abstract class MixinFontRenderer {

    @Shadow
    private boolean randomStyle;
    @Shadow private boolean boldStyle;
    @Shadow private boolean strikethroughStyle;
    @Shadow private boolean underlineStyle;
    @Shadow private boolean italicStyle;
    @Shadow private int textColor;
    @Shadow private float alpha;
    @Shadow private float red;
    /** Actually green */
    @Shadow private float blue;
    /** Actually blue */
    @Shadow private float green;
    @Shadow protected float posX;
    @Shadow protected float posY;
    @Shadow public int FONT_HEIGHT;

    @Shadow(remap = false)
    protected abstract void setColor(float r, float g, float b, float a);

    @Unique
    private LegacyRenderTextData hextext$legacyRenderData;
    @Unique
    private Deque<Integer> hextext$legacyColorStack;
    @Unique
    private int hextext$legacyBaseColor;
    @Unique
    private boolean hextext$legacyShadow;
    @Unique
    private List<TokenHighlight> hextext$pendingHighlights;
    @Unique
    private int hextext$rawTokenSkip;
    @Unique
    private boolean hextext$renderingShadow;

    @Inject(method = "renderStringAtPos", at = @At("HEAD"))
    private void hextext$legacy$begin(String text, boolean shadow, CallbackInfo ci) {
        hextext$legacyRenderData = hextext$prepareRenderData(text);
        hextext$legacyColorStack = null;
        hextext$legacyBaseColor = this.textColor;
        hextext$legacyShadow = shadow;
        hextext$renderingShadow = shadow;
        if (!shadow && LegacyFontRenderContext.isRawTextRendering()) {
            if (hextext$pendingHighlights == null) {
                hextext$pendingHighlights = new ArrayList<>();
            } else {
                hextext$pendingHighlights.clear();
            }
        } else if (hextext$pendingHighlights != null) {
            hextext$pendingHighlights.clear();
        }
        hextext$rawTokenSkip = 0;
    }

    @ModifyVariable(method = "renderStringAtPos", at = @At("HEAD"), argsOnly = true)
    private String hextext$legacy$replaceRenderText(String text) {
        if (hextext$legacyRenderData != null && hextext$legacyRenderData.isModified()) {
            return hextext$legacyRenderData.getSanitized();
        }
        return text;
    }

    @Inject(method = "renderStringAtPos", at = @At(value = "INVOKE_ASSIGN", target = "Ljava/lang/String;charAt(I)C"),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private void hextext$legacy$applyActions(String text, boolean shadow, CallbackInfo ci, int index, char current) {
        if (!hextext$renderingShadow && LegacyFontRenderContext.isRawTextRendering()) {
            if (hextext$rawTokenSkip > 0) {
                hextext$rawTokenSkip--;
            } else {
                int tokenLength = ColorCodeUtils.detectColorCodeLengthIgnoringRaw(text, index);
                if (tokenLength > 0) {
                    float width = hextext$measureLiteralWidth(text, index, tokenLength);
                    if (width > 0.0f && hextext$pendingHighlights != null) {
                        hextext$pendingHighlights.add(new TokenHighlight(this.posX, this.posY, width,
                            hextext$getTokenHighlightColor(text, index)));
                    }
                    hextext$rawTokenSkip = Math.max(tokenLength - 1, 0);
                }
            }
        }

        if (hextext$legacyRenderData == null || hextext$legacyRenderData.getActions() == null) {
            return;
        }

        List<LegacyRenderAction> actions = hextext$legacyRenderData.getActions().remove(index);
        if (actions == null) {
            return;
        }

        for (LegacyRenderAction action : actions) {
            hextext$execute(action);
        }
    }

    @Inject(method = "renderStringAtPos", at = @At("TAIL"))
    private void hextext$legacy$end(String text, boolean shadow, CallbackInfo ci) {
        hextext$legacyRenderData = null;
        hextext$legacyColorStack = null;
        if (!hextext$renderingShadow && hextext$pendingHighlights != null && !hextext$pendingHighlights.isEmpty()) {
            hextext$drawTokenHighlights();
            hextext$pendingHighlights.clear();
        }
    }

    @Inject(method = "getStringWidth", at = @At("RETURN"), cancellable = true)
    private void hextext$legacy$width(String text, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue((int) hextext$calculateMaxLineWidth(text));
    }

    @Inject(method = "sizeStringToWidth", at = @At("RETURN"), cancellable = true)
    private void hextext$legacy$size(String text, int maxWidth, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(hextext$computeLineBreakIndex(text, maxWidth));
    }

    @Inject(method = "trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void hextext$legacy$trim(String text, int width, boolean reverse, CallbackInfoReturnable<String> cir) {
        if (text == null || text.isEmpty()) {
            cir.setReturnValue("");
            return;
        }

        if (!reverse) {
            int endIndex = hextext$computeLineBreakIndex(text, width);
            cir.setReturnValue(text.substring(0, Math.min(endIndex, text.length())));
            return;
        }

        cir.setReturnValue(hextext$trimFromEnd(text, width));
    }

    @Inject(method = "wrapFormattedStringToWidth", at = @At("RETURN"), cancellable = true)
    private void hextext$legacy$wrap(String text, int width, CallbackInfoReturnable<String> cir) {
        if (text == null || text.isEmpty()) {
            cir.setReturnValue("");
            return;
        }

        cir.setReturnValue(hextext$wrapFormattedStringToWidth(text, width));
    }

    @Inject(method = "getFormatFromString", at = @At("RETURN"), cancellable = true)
    private static void hextext$legacy$format(String text, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ColorCodeUtils.extractFormatFromString(text));
    }

    @Unique
    private void hextext$execute(LegacyRenderAction action) {
        switch (action.getType()) {
            case APPLY_RGB:
                if (action.shouldClearStack() && hextext$legacyColorStack != null) {
                    hextext$legacyColorStack.clear();
                }
                hextext$applyRgbColor(action.getRgb(), hextext$legacyShadow);
                hextext$resetFormattingStyles();
                break;
            case PUSH_RGB:
                if (hextext$legacyColorStack == null) {
                    hextext$legacyColorStack = new ArrayDeque<>();
                }
                hextext$legacyColorStack.push(this.textColor);
                hextext$applyRgbColor(action.getRgb(), hextext$legacyShadow);
                break;
            case POP_COLOR:
                if (hextext$legacyColorStack != null && !hextext$legacyColorStack.isEmpty()) {
                    hextext$setColorFromInt(hextext$legacyColorStack.pop());
                } else {
                    hextext$setColorFromInt(hextext$legacyBaseColor);
                }
                break;
        }
    }

    @Unique
    private void hextext$setColorFromInt(int rgb) {
        this.textColor = rgb;
        setColor((float) (rgb >> 16 & 255) / 255.0F,
            (float) (rgb >> 8 & 255) / 255.0F,
            (float) (rgb & 255) / 255.0F,
            this.alpha);
    }

    @Unique
    private void hextext$applyRgbColor(int rgb, boolean shadow) {
        int effective = shadow ? ColorCodeUtils.calculateShadowColor(rgb) : rgb;
        hextext$setColorFromInt(effective);
    }

    @Unique
    private void hextext$resetFormattingStyles() {
        this.randomStyle = false;
        this.boldStyle = false;
        this.strikethroughStyle = false;
        this.underlineStyle = false;
        this.italicStyle = false;
    }

    @Unique
    private LegacyRenderTextData hextext$prepareRenderData(String text) {
        if (text == null || text.isEmpty() || LegacyFontRenderContext.isRawTextRendering()) {
            return LegacyRenderTextData.unmodified(text);
        }

        StringBuilder sanitized = new StringBuilder(text.length());
        Map<Integer, List<LegacyRenderAction>> actions = null;
        boolean modified = false;

        for (int i = 0; i < text.length(); ++i) {
            char current = text.charAt(i);

            if (current == '&' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (ColorCodeUtils.isFormattingCode(next)) {
                    sanitized.append('§');
                    modified = true;
                    continue;
                }

                if (ColorCodeUtils.isValidHexString(text, i + 1)) {
                    int rgb = ColorCodeUtils.parseHexColor(text, i + 1);
                    if (rgb != -1) {
                        if (actions == null) {
                            actions = new HashMap<>();
                        }
                        actions.computeIfAbsent(sanitized.length(), key -> new ArrayList<>())
                            .add(LegacyRenderAction.apply(rgb, true));
                        modified = true;
                        i += 6;
                        continue;
                    }
                }
            }

            if (current == '<') {
                if (i + 8 <= text.length() && text.charAt(i + 7) == '>' && ColorCodeUtils.isValidHexString(text, i + 1)) {
                    int rgb = ColorCodeUtils.parseHexColor(text, i + 1);
                    if (rgb != -1) {
                        if (actions == null) {
                            actions = new HashMap<>();
                        }
                        actions.computeIfAbsent(sanitized.length(), key -> new ArrayList<>())
                            .add(LegacyRenderAction.push(rgb));
                        modified = true;
                        i += 7;
                        continue;
                    }
                }

                if (i + 9 <= text.length() && text.charAt(i + 1) == '/' && text.charAt(i + 8) == '>'
                    && ColorCodeUtils.isValidHexString(text, i + 2)) {
                    if (actions == null) {
                        actions = new HashMap<>();
                    }
                    actions.computeIfAbsent(sanitized.length(), key -> new ArrayList<>())
                        .add(LegacyRenderAction.pop());
                    modified = true;
                    i += 8;
                    continue;
                }
            }

            sanitized.append(current);
        }

        if (!modified) {
            return LegacyRenderTextData.unmodified(text);
        }

        return LegacyRenderTextData.modified(sanitized.toString(), actions);
    }

    @Unique
    private float hextext$measureLiteralWidth(CharSequence text, int start, int length) {
        if (length <= 0 || start < 0 || start >= text.length()) {
            return 0.0f;
        }
        int end = Math.min(start + length, text.length());
        if (end <= start) {
            return 0.0f;
        }
        String segment = text.subSequence(start, end).toString();
        return ((FontRenderer) (Object) this).getStringWidth(segment);
    }

    @Unique
    private int hextext$getTokenHighlightColor(CharSequence text, int index) {
        char c = text.charAt(index);
        if (c == 167 || (c == '&' && index + 1 < text.length() && ColorCodeUtils.isFormattingCode(text.charAt(index + 1)))) {
            return 0x304080FF;
        }
        if (c == '&') {
            return 0x3039C86F;
        }
        if (c == '<') {
            if (index + 1 < text.length() && text.charAt(index + 1) == '/') {
                return 0x30FF8C5A;
            }
            return 0x305A8CFF;
        }
        return 0x30222222;
    }

    @Unique
    private void hextext$drawTokenHighlights() {
        for (TokenHighlight highlight : hextext$pendingHighlights) {
            hextext$drawTokenHighlight(highlight);
        }
    }

    @Unique
    private void hextext$drawTokenHighlight(TokenHighlight highlight) {
        float left = highlight.x;
        float right = highlight.x + highlight.width;
        float top = highlight.y - 1.0f;
        float bottom = highlight.y + this.FONT_HEIGHT;

        int x1 = MathHelper.floor_float(left);
        int y1 = MathHelper.floor_float(top);
        int x2 = MathHelper.ceiling_float_int(right);
        int y2 = MathHelper.ceiling_float_int(bottom);

        Gui.drawRect(x1, y1, x2, y2, highlight.color);
    }

    @Unique
    private float hextext$getCharWidthFloat(char chr) {
        if (chr == 167) {
            return 0.0f;
        }
        int width = ((FontRenderer) (Object) this).getCharWidth(chr);
        return width;
    }

    @Unique
    private float hextext$calculateMaxLineWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }

        final boolean rawMode = LegacyFontRenderContext.isRawTextRendering();
        boolean bold = false;
        float lineWidth = 0.0f;
        float maxWidth = 0.0f;

        for (int index = 0; index < text.length(); ++index) {
            if (!rawMode) {
                int codeLen = ColorCodeUtils.detectColorCodeLength(text, index);
                if (codeLen > 0) {
                    if (codeLen == 2 && index + 1 < text.length()) {
                        char fmt = Character.toLowerCase(text.charAt(index + 1));
                        if (fmt == 'l') {
                            bold = true;
                        } else if (fmt == 'r'
                            || (fmt >= '0' && fmt <= '9')
                            || (fmt >= 'a' && fmt <= 'f')) {
                            bold = false;
                        }
                    } else if (codeLen == 7 || codeLen == 8) {
                        bold = false;
                    }
                    index += codeLen - 1;
                    continue;
                }
            }

            char chr = text.charAt(index);
            if (chr == '\n') {
                maxWidth = Math.max(maxWidth, lineWidth);
                lineWidth = 0.0f;
                continue;
            }

            float width = hextext$getCharWidthFloat(chr);
            lineWidth += width;
            if (bold && width > 0.0f) {
                lineWidth += 1.0f;
            }
            maxWidth = Math.max(maxWidth, lineWidth);
        }

        return Math.max(maxWidth, lineWidth);
    }

    @Unique
    private int hextext$computeLineBreakIndex(String text, int maxWidth) {
        return FormattedTextMetrics.computeLineBreakIndex(text, maxWidth,
            LegacyFontRenderContext.isRawTextRendering(), this::hextext$getCharWidthFloat, 0.0f, 1.0f);
    }

    @Unique
    private String hextext$trimFromEnd(String text, int width) {
        final boolean rawMode = LegacyFontRenderContext.isRawTextRendering();
        float currentWidth = 0.0f;
        int firstSafePosition = text.length();
        boolean bold = false;

        for (int index = text.length() - 1; index >= 0; ) {
            char chr = text.charAt(index);

            if (!rawMode) {
                if (index >= 6 && text.charAt(index - 6) == '&') {
                    boolean validHex = true;
                    for (int scan = index - 5; scan <= index; scan++) {
                        if (!ColorCodeUtils.isValidHexChar(text.charAt(scan))) {
                            validHex = false;
                            break;
                        }
                    }
                    if (validHex) {
                        index -= 7;
                        firstSafePosition = index + 1;
                        bold = false;
                        continue;
                    }
                }

                if (index >= 1 && text.charAt(index - 1) == 167) {
                    char fmt = Character.toLowerCase(chr);
                    if (fmt == 'l') {
                        bold = true;
                    } else if (fmt == 'r'
                        || (fmt >= '0' && fmt <= '9')
                        || (fmt >= 'a' && fmt <= 'f')) {
                        bold = false;
                    }
                    index -= 2;
                    firstSafePosition = index + 1;
                    continue;
                }

                if (index >= 7 && text.charAt(index - 7) == '<' && text.charAt(index) == '>') {
                    if (ColorCodeUtils.isValidHexString(text, index - 6)) {
                        index -= 8;
                        firstSafePosition = index + 1;
                        bold = false;
                        continue;
                    }
                }

                if (index >= 8 && text.charAt(index - 8) == '<' && text.charAt(index - 7) == '/' && text.charAt(index) == '>') {
                    if (ColorCodeUtils.isValidHexString(text, index - 6)) {
                        index -= 9;
                        firstSafePosition = index + 1;
                        bold = false;
                        continue;
                    }
                }

                if (index >= 1 && text.charAt(index - 1) == '&') {
                    char fmt = Character.toLowerCase(chr);
                    if (ColorCodeUtils.isFormattingCode(fmt)) {
                        if (fmt == 'l') {
                            bold = true;
                        } else if (fmt == 'r'
                            || (fmt >= '0' && fmt <= '9')
                            || (fmt >= 'a' && fmt <= 'f')) {
                            bold = false;
                        }
                        index -= 2;
                        firstSafePosition = index + 1;
                        continue;
                    }
                }
            }

            if (chr == '\n') {
                return text.substring(index + 1);
            }

            float glyphWidth = hextext$getCharWidthFloat(chr);
            if (glyphWidth < 0.0f) {
                glyphWidth = 0.0f;
            }

            float nextWidth = currentWidth + glyphWidth;
            if (bold && glyphWidth > 0.0f) {
                nextWidth += 1.0f;
            }

            if (nextWidth > width) {
                return text.substring(firstSafePosition);
            }

            currentWidth = nextWidth;
            firstSafePosition = index;
            index--;
        }

        return text;
    }

    @Unique
    private String hextext$wrapFormattedStringToWidth(String text, int wrapWidth) {
        int breakPoint = hextext$computeLineBreakIndex(text, wrapWidth);

        if (breakPoint >= text.length()) {
            return text;
        }

        String firstPart = text.substring(0, breakPoint);
        char breakChar = text.charAt(breakPoint);
        boolean skipChar = breakChar == ' ' || breakChar == '\n';

        String remainder = ColorCodeUtils.extractFormatFromString(firstPart)
            + text.substring(breakPoint + (skipChar ? 1 : 0));

        if (remainder.length() == text.length()) {
            return firstPart + "\n" + remainder;
        }

        return firstPart + "\n" + hextext$wrapFormattedStringToWidth(remainder, wrapWidth);
    }
}
