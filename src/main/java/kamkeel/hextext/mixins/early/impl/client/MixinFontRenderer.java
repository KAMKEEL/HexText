package kamkeel.hextext.mixins.early.impl.client;

import kamkeel.hextext.client.ColorStateTracker;
import kamkeel.hextext.client.FontRenderContext;
import kamkeel.hextext.client.FontRendererUtils;
import kamkeel.hextext.client.RenderInstruction;
import kamkeel.hextext.client.RenderTextData;
import kamkeel.hextext.client.RenderTextProcessor;
import kamkeel.hextext.client.TokenHighlight;
import kamkeel.hextext.client.TextEffectController;
import kamkeel.hextext.client.TokenHighlightUtils;
import kamkeel.hextext.util.ColorCodeUtils;
import kamkeel.hextext.util.StringUtils;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;

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
    @Shadow private int[] colorCode;

    @Shadow(remap = false)
    protected abstract void setColor(float r, float g, float b, float a);

    @Invoker("renderDefaultChar")
    protected abstract float hextext$invokeRenderDefaultChar(int character, boolean italic);

    @Invoker("renderUnicodeChar")
    protected abstract float hextext$invokeRenderUnicodeChar(char character, boolean italic);

    @Unique
    private RenderTextData hextext$renderData;
    @Unique
    private ColorStateTracker hextext$colorState;
    @Unique
    private boolean hextext$shadow;
    @Unique
    private List<TokenHighlight> hextext$pendingHighlights;
    @Unique
    private int hextext$rawTokenSkip;
    @Unique
    private boolean hextext$renderingShadow;
    @Unique
    private TextEffectController hextext$effects;
    @Unique
    private int hextext$visibleGlyphIndex;
    @Unique
    private int hextext$pendingRenderColor;
    @Unique
    private boolean hextext$hasPendingRenderColor;

    @Inject(method = "renderString", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;setColor(FFFF)V", shift = At.Shift.AFTER, remap = false), locals = LocalCapture.CAPTURE_FAILHARD)
    private void hextext$capturePreparedColor(String text, int x, int y, int color, boolean dropShadow,
            CallbackInfoReturnable<Integer> cir) {
        hextext$pendingRenderColor = color & 0xFFFFFF;
        hextext$hasPendingRenderColor = true;
    }

    @Inject(method = "renderStringAtPos", at = @At("HEAD"))
    private void hextext$begin(String text, boolean shadow, CallbackInfo ci) {
        boolean rawMode = FontRenderContext.isRawTextRendering();
        hextext$renderData = RenderTextProcessor.prepare(text, rawMode);
        if (hextext$colorState == null) {
            hextext$colorState = new ColorStateTracker();
        }
        hextext$shadow = shadow;
        hextext$renderingShadow = shadow;
        if (hextext$effects == null) {
            hextext$effects = new TextEffectController();
        }
        int initialColor = hextext$resolveInitialColor();
        hextext$colorState.begin(initialColor, shadow);
        this.textColor = initialColor;
        hextext$effects.begin(initialColor);
        if (rawMode) {
            hextext$resetFormattingStyles();
        }
        if (!shadow && rawMode) {
            if (hextext$pendingHighlights == null) {
                hextext$pendingHighlights = new ArrayList<>();
            } else {
                hextext$pendingHighlights.clear();
            }
        } else if (hextext$pendingHighlights != null) {
            hextext$pendingHighlights.clear();
        }
        hextext$rawTokenSkip = 0;
        hextext$visibleGlyphIndex = 0;
    }

    @ModifyVariable(method = "renderStringAtPos", at = @At("HEAD"), argsOnly = true)
    private String hextext$replaceRenderText(String text) {
        if (hextext$renderData != null && hextext$renderData.shouldReplaceText()) {
            return hextext$renderData.getDisplayText();
        }
        return text;
    }

    @Inject(method = "renderStringAtPos", at = @At(value = "INVOKE_ASSIGN", target = "Ljava/lang/String;charAt(I)C"),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private void hextext$applyInstructions(String text, boolean shadow, CallbackInfo ci, int index, char current) {
        if (!hextext$renderingShadow && FontRenderContext.isRawTextRendering()) {
            if (hextext$rawTokenSkip > 0) {
                hextext$rawTokenSkip--;
            } else {
                int tokenLength = ColorCodeUtils.detectColorCodeLengthIgnoringRaw(text, index);
                if (tokenLength > 0) {
                    if (text.charAt(index) != 167) {
                        float width = TokenHighlightUtils.measureLiteralWidth((FontRenderer) (Object) this, text, index, tokenLength);
                        if (width > 0.0f && hextext$pendingHighlights != null) {
                            hextext$pendingHighlights.add(new TokenHighlight(this.posX, this.posY, width,
                                TokenHighlightUtils.getTokenHighlightColor(text, index)));
                        }
                        hextext$rawTokenSkip = Math.max(tokenLength - 1, 0);
                    }
                }
            }
        }

        if (hextext$renderData == null || !hextext$renderData.hasInstructions()) {
            return;
        }

        List<RenderInstruction> instructions = hextext$renderData.getInstructions().remove(index);
        if (instructions == null) {
            return;
        }

        for (RenderInstruction instruction : instructions) {
            hextext$executeInstruction(instruction);
        }
    }

    @Inject(method = "renderStringAtPos", at = @At("TAIL"))
    private void hextext$end(String text, boolean shadow, CallbackInfo ci) {
        hextext$renderData = null;
        if (!hextext$renderingShadow && hextext$pendingHighlights != null && !hextext$pendingHighlights.isEmpty()) {
            TokenHighlightUtils.drawHighlights(hextext$pendingHighlights, this.FONT_HEIGHT);
            hextext$pendingHighlights.clear();
        }
    }

    @Inject(method = "getStringWidth", at = @At("RETURN"), cancellable = true)
    private void hextext$width(String text, CallbackInfoReturnable<Integer> cir) {
        boolean rawMode = FontRenderContext.isRawTextRendering();
        int value = (int) FontRendererUtils.calculateMaxLineWidth((FontRenderer) (Object) this, text, rawMode);
        cir.setReturnValue(value);
    }

    @Inject(method = "sizeStringToWidth", at = @At("RETURN"), cancellable = true)
    private void hextext$size(String text, int maxWidth, CallbackInfoReturnable<Integer> cir) {
        boolean rawMode = FontRenderContext.isRawTextRendering();
        int index = FontRendererUtils.computeLineBreakIndex((FontRenderer) (Object) this, text, maxWidth, rawMode);
        cir.setReturnValue(index);
    }

    @Inject(method = "trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void hextext$trim(String text, int width, boolean reverse, CallbackInfoReturnable<String> cir) {
        if (text == null || text.isEmpty()) {
            cir.setReturnValue("");
            return;
        }

        if (!reverse) {
            boolean rawMode = FontRenderContext.isRawTextRendering();
            cir.setReturnValue(FontRendererUtils.trimStringToWidthForward((FontRenderer) (Object) this, text, width, rawMode));
            return;
        }

        boolean rawMode = FontRenderContext.isRawTextRendering();
        cir.setReturnValue(FontRendererUtils.trimStringFromEnd((FontRenderer) (Object) this, text, width, rawMode));
    }

    @Inject(method = "wrapFormattedStringToWidth", at = @At("RETURN"), cancellable = true)
    private void hextext$wrap(String text, int width, CallbackInfoReturnable<String> cir) {
        if (text == null || text.isEmpty()) {
            cir.setReturnValue("");
            return;
        }

        boolean rawMode = FontRenderContext.isRawTextRendering();
        cir.setReturnValue(FontRendererUtils.wrapFormattedString((FontRenderer) (Object) this, text, width, rawMode));
    }

    @Inject(method = "getFormatFromString", at = @At("RETURN"), cancellable = true)
    private static void hextext$format(String text, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(StringUtils.extractFormatFromString(text));
    }

    @SuppressWarnings("unused")
    @Redirect(method = "renderCharAtPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;renderDefaultChar(IZ)F"))
    private float hextext$renderDefaultChar(FontRenderer fontRenderer, int character, boolean italic,
            int glyphIndex, char glyph, boolean italicFlag) {
        return hextext$renderGlyphWithEffects(glyph, italic, false, character, glyph);
    }

    @SuppressWarnings("unused")
    @Redirect(method = "renderCharAtPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;renderUnicodeChar(CZ)F"))
    private float hextext$renderUnicodeChar(FontRenderer fontRenderer, char character, boolean italic,
            int glyphIndex, char glyph, boolean italicFlag) {
        return hextext$renderGlyphWithEffects(glyph, italic, true, 0, character);
    }

    @Unique
    private float hextext$renderGlyphWithEffects(char glyph, boolean italic, boolean unicode, int defaultIndex, char unicodeChar) {
        int glyphIndex = hextext$visibleGlyphIndex;
        float width;
        if (hextext$effects != null && hextext$effects.hasActiveEffects()) {
            int targetColor = hextext$effects.computeColor(glyphIndex);
            int appliedColor = hextext$shadow ? ColorCodeUtils.calculateShadowColor(targetColor) : targetColor;
            hextext$setColorFromInt(appliedColor);
            hextext$effects.beforeGlyph((FontRenderer) (Object) this, glyph, glyphIndex, this.posX, this.posY, this.FONT_HEIGHT);
            width = unicode
                ? hextext$invokeRenderUnicodeChar(unicodeChar, italic)
                : hextext$invokeRenderDefaultChar(defaultIndex, italic);
            hextext$effects.afterGlyph();
        } else {
            width = unicode
                ? hextext$invokeRenderUnicodeChar(unicodeChar, italic)
                : hextext$invokeRenderDefaultChar(defaultIndex, italic);
        }
        return width;
    }

    @Inject(method = "doDraw", at = @At("TAIL"), remap = false)
    private void hextext$advanceVisibleGlyphIndex(float width, CallbackInfo ci) {
        hextext$visibleGlyphIndex++;
    }

    @Unique
    private void hextext$executeInstruction(RenderInstruction instruction) {
        boolean resetStyles = instruction.resetsFormatting();

        switch (instruction.getType()) {
            case APPLY_RGB:
                int appliedRgb = hextext$colorState.applyRgb(instruction.getRgb(), instruction.shouldClearStack(),
                    hextext$effects, hextext$renderingShadow);
                hextext$setColorFromInt(appliedRgb);
                break;
            case APPLY_VANILLA_COLOR:
                int vanillaColor = hextext$colorState.applyVanillaColor(instruction.getParameter(), colorCode,
                    instruction.shouldClearStack(), hextext$effects, hextext$renderingShadow);
                hextext$setColorFromInt(vanillaColor);
                break;
            case PUSH_RGB:
                int pushedColor = hextext$colorState.push(instruction.getRgb(), hextext$effects, hextext$renderingShadow);
                hextext$setColorFromInt(pushedColor);
                break;
            case POP_COLOR:
                int restoredColor = hextext$colorState.pop(hextext$effects, hextext$renderingShadow);
                hextext$setColorFromInt(restoredColor);
                break;
            case RESET_TO_BASE:
                int baseColor = hextext$colorState.resetToBase(hextext$effects, hextext$renderingShadow);
                hextext$setColorFromInt(baseColor);
                break;
            case SET_RANDOM:
                this.randomStyle = instruction.isEnabled();
                break;
            case SET_BOLD:
                this.boldStyle = instruction.isEnabled();
                break;
            case SET_STRIKETHROUGH:
                this.strikethroughStyle = instruction.isEnabled();
                break;
            case SET_UNDERLINE:
                this.underlineStyle = instruction.isEnabled();
                break;
            case SET_ITALIC:
                this.italicStyle = instruction.isEnabled();
                break;
            case SET_RAINBOW:
                if (instruction.shouldClearStack()) {
                    hextext$colorState.clearStacks();
                }
                if (hextext$effects != null) {
                    hextext$effects.resetDynamicEffects();
                    hextext$effects.setRainbow(instruction.isEnabled(), hextext$visibleGlyphIndex);
                    if (!hextext$renderingShadow) {
                        hextext$effects.updateBaseColor(this.textColor);
                    }
                }
                resetStyles = true;
                break;
            case SET_DINNERBONE:
                if (hextext$effects != null) {
                    hextext$effects.setDinnerbone(instruction.isEnabled());
                }
                break;
            case SET_IGNITE:
                if (hextext$effects != null) {
                    if (instruction.isEnabled() && !hextext$renderingShadow) {
                        hextext$effects.updateBaseColor(this.textColor);
                    }
                    hextext$effects.setIgnite(instruction.isEnabled());
                }
                break;
            case SET_SHAKE:
                if (hextext$effects != null) {
                    hextext$effects.setShake(instruction.isEnabled());
                }
                break;
        }

        if (resetStyles) {
            hextext$resetFormattingStyles();
        }
    }

    @Unique
    private void hextext$setColorFromInt(int rgb) {
        int masked = rgb & 0xFFFFFF;
        this.textColor = masked;
        if (hextext$colorState != null) {
            hextext$colorState.setCurrentColor(masked);
        }
        setColor((float) (rgb >> 16 & 255) / 255.0F,
            (float) (rgb >> 8 & 255) / 255.0F,
            (float) (rgb & 255) / 255.0F,
            this.alpha);
    }

    @Unique
    private int hextext$resolveInitialColor() {
        if (hextext$hasPendingRenderColor) {
            hextext$hasPendingRenderColor = false;
            return hextext$pendingRenderColor & 0xFFFFFF;
        }
        int r = Math.round(this.red * 255.0f);
        int g = Math.round(this.blue * 255.0f);
        int b = Math.round(this.green * 255.0f);
        return (r << 16) | (g << 8) | b;
    }

    @Unique
    private void hextext$resetFormattingStyles() {
        this.randomStyle = false;
        this.boldStyle = false;
        this.strikethroughStyle = false;
        this.underlineStyle = false;
        this.italicStyle = false;
    }

}
