package kamkeel.hextext.mixins.early.impl.client;

import kamkeel.hextext.ColorCodeUtils;
import kamkeel.hextext.client.FormattedTextMetrics;
import kamkeel.hextext.client.LegacyFontRenderContext;
import kamkeel.hextext.client.LegacyRenderAction;
import kamkeel.hextext.client.LegacyRenderTextData;
import net.minecraft.client.gui.FontRenderer;
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

    @Shadow(remap = false)
    protected abstract void setColor(float r, float g, float b, float a);

    @Unique
    private LegacyRenderTextData angelica$legacyRenderData;
    @Unique
    private Deque<Integer> angelica$legacyColorStack;
    @Unique
    private int angelica$legacyBaseColor;
    @Unique
    private boolean angelica$legacyShadow;

    @Inject(method = "renderStringAtPos", at = @At("HEAD"))
    private void angelica$legacy$begin(String text, boolean shadow, CallbackInfo ci) {
        angelica$legacyRenderData = angelica$prepareRenderData(text);
        angelica$legacyColorStack = null;
        angelica$legacyBaseColor = this.textColor;
        angelica$legacyShadow = shadow;
    }

    @ModifyVariable(method = "renderStringAtPos", at = @At("HEAD"), argsOnly = true)
    private String angelica$legacy$replaceRenderText(String text) {
        if (angelica$legacyRenderData != null && angelica$legacyRenderData.isModified()) {
            return angelica$legacyRenderData.getSanitized();
        }
        return text;
    }

    @Inject(method = "renderStringAtPos", at = @At(value = "INVOKE_ASSIGN", target = "Ljava/lang/String;charAt(I)C"),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private void angelica$legacy$applyActions(String text, boolean shadow, CallbackInfo ci, int index, char current) {
        if (angelica$legacyRenderData == null || angelica$legacyRenderData.getActions() == null) {
            return;
        }

        List<LegacyRenderAction> actions = angelica$legacyRenderData.getActions().remove(index);
        if (actions == null) {
            return;
        }

        for (LegacyRenderAction action : actions) {
            angelica$execute(action);
        }
    }

    @Inject(method = "renderStringAtPos", at = @At("TAIL"))
    private void angelica$legacy$end(String text, boolean shadow, CallbackInfo ci) {
        angelica$legacyRenderData = null;
        angelica$legacyColorStack = null;
    }

    @Inject(method = "getStringWidth", at = @At("RETURN"), cancellable = true)
    private void angelica$legacy$width(String text, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue((int) angelica$calculateMaxLineWidth(text));
    }

    @Inject(method = "sizeStringToWidth", at = @At("RETURN"), cancellable = true)
    private void angelica$legacy$size(String text, int maxWidth, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(angelica$computeLineBreakIndex(text, maxWidth));
    }

    @Inject(method = "trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void angelica$legacy$trim(String text, int width, boolean reverse, CallbackInfoReturnable<String> cir) {
        if (text == null || text.isEmpty()) {
            cir.setReturnValue("");
            return;
        }

        if (!reverse) {
            int endIndex = angelica$computeLineBreakIndex(text, width);
            cir.setReturnValue(text.substring(0, Math.min(endIndex, text.length())));
            return;
        }

        cir.setReturnValue(angelica$trimFromEnd(text, width));
    }

    @Inject(method = "wrapFormattedStringToWidth", at = @At("RETURN"), cancellable = true)
    private void angelica$legacy$wrap(String text, int width, CallbackInfoReturnable<String> cir) {
        if (text == null || text.isEmpty()) {
            cir.setReturnValue("");
            return;
        }

        cir.setReturnValue(angelica$wrapFormattedStringToWidth(text, width));
    }

    @Inject(method = "getFormatFromString", at = @At("RETURN"), cancellable = true)
    private static void angelica$legacy$format(String text, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ColorCodeUtils.extractFormatFromString(text));
    }

    @Unique
    private void angelica$execute(LegacyRenderAction action) {
        switch (action.getType()) {
            case APPLY_RGB:
                if (action.shouldClearStack() && angelica$legacyColorStack != null) {
                    angelica$legacyColorStack.clear();
                }
                angelica$applyRgbColor(action.getRgb(), angelica$legacyShadow);
                angelica$resetFormattingStyles();
                break;
            case PUSH_RGB:
                if (angelica$legacyColorStack == null) {
                    angelica$legacyColorStack = new ArrayDeque<>();
                }
                angelica$legacyColorStack.push(this.textColor);
                angelica$applyRgbColor(action.getRgb(), angelica$legacyShadow);
                break;
            case POP_COLOR:
                if (angelica$legacyColorStack != null && !angelica$legacyColorStack.isEmpty()) {
                    angelica$setColorFromInt(angelica$legacyColorStack.pop());
                } else {
                    angelica$setColorFromInt(angelica$legacyBaseColor);
                }
                break;
        }
    }

    @Unique
    private void angelica$setColorFromInt(int rgb) {
        this.textColor = rgb;
        setColor((float) (rgb >> 16 & 255) / 255.0F,
            (float) (rgb >> 8 & 255) / 255.0F,
            (float) (rgb & 255) / 255.0F,
            this.alpha);
    }

    @Unique
    private void angelica$applyRgbColor(int rgb, boolean shadow) {
        int effective = shadow ? ColorCodeUtils.calculateShadowColor(rgb) : rgb;
        angelica$setColorFromInt(effective);
    }

    @Unique
    private void angelica$resetFormattingStyles() {
        this.randomStyle = false;
        this.boldStyle = false;
        this.strikethroughStyle = false;
        this.underlineStyle = false;
        this.italicStyle = false;
    }

    @Unique
    private LegacyRenderTextData angelica$prepareRenderData(String text) {
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
    private float angelica$getCharWidthFloat(char chr) {
        if (chr == 167) {
            return 0.0f;
        }
        int width = ((FontRenderer) (Object) this).getCharWidth(chr);
        return width;
    }

    @Unique
    private float angelica$calculateMaxLineWidth(String text) {
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

            float width = angelica$getCharWidthFloat(chr);
            lineWidth += width;
            if (bold && width > 0.0f) {
                lineWidth += 1.0f;
            }
            maxWidth = Math.max(maxWidth, lineWidth);
        }

        return Math.max(maxWidth, lineWidth);
    }

    @Unique
    private int angelica$computeLineBreakIndex(String text, int maxWidth) {
        return FormattedTextMetrics.computeLineBreakIndex(text, maxWidth,
            LegacyFontRenderContext.isRawTextRendering(), this::angelica$getCharWidthFloat, 0.0f, 1.0f);
    }

    @Unique
    private String angelica$trimFromEnd(String text, int width) {
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

            float glyphWidth = angelica$getCharWidthFloat(chr);
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
    private String angelica$wrapFormattedStringToWidth(String text, int wrapWidth) {
        int breakPoint = angelica$computeLineBreakIndex(text, wrapWidth);

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

        return firstPart + "\n" + angelica$wrapFormattedStringToWidth(remainder, wrapWidth);
    }
}
