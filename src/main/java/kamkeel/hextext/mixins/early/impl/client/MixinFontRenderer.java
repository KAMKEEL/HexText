package kamkeel.hextext.mixins.early.impl.client;

import kamkeel.hextext.client.FontRenderContext;
import kamkeel.hextext.client.FontRendererUtils;
import kamkeel.hextext.client.RenderInstruction;
import kamkeel.hextext.client.RenderTextData;
import kamkeel.hextext.client.RenderTextProcessor;
import kamkeel.hextext.client.TokenHighlight;
import kamkeel.hextext.client.TokenHighlightUtils;
import kamkeel.hextext.util.ColorCodeUtils;
import kamkeel.hextext.util.StringUtils;
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

    @Unique
    private RenderTextData hextext$renderData;
    @Unique
    private Deque<Integer> hextext$colorStack;
    @Unique
    private int hextext$baseColor;
    @Unique
    private boolean hextext$shadow;
    @Unique
    private List<TokenHighlight> hextext$pendingHighlights;
    @Unique
    private int hextext$rawTokenSkip;
    @Unique
    private boolean hextext$renderingShadow;

    @Inject(method = "renderStringAtPos", at = @At("HEAD"))
    private void hextext$begin(String text, boolean shadow, CallbackInfo ci) {
        boolean rawMode = FontRenderContext.isRawTextRendering();
        hextext$renderData = RenderTextProcessor.prepare(text, rawMode);
        hextext$colorStack = null;
        hextext$baseColor = this.textColor;
        hextext$shadow = shadow;
        hextext$renderingShadow = shadow;
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
        hextext$colorStack = null;
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
            int endIndex = FontRendererUtils.computeLineBreakIndex((FontRenderer) (Object) this, text, width, rawMode);
            cir.setReturnValue(text.substring(0, Math.min(endIndex, text.length())));
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

    @Unique
    private void hextext$executeInstruction(RenderInstruction instruction) {
        switch (instruction.getType()) {
            case APPLY_RGB:
                if (instruction.shouldClearStack() && hextext$colorStack != null) {
                    hextext$colorStack.clear();
                }
                hextext$applyRgbColor(instruction.getRgb(), hextext$shadow);
                hextext$resetFormattingStyles();
                break;
            case APPLY_VANILLA_COLOR:
                if (instruction.shouldClearStack() && hextext$colorStack != null) {
                    hextext$colorStack.clear();
                }
                hextext$applyVanillaColor(instruction.getParameter());
                hextext$resetFormattingStyles();
                break;
            case PUSH_RGB:
                if (hextext$colorStack == null) {
                    hextext$colorStack = new ArrayDeque<>();
                }
                hextext$colorStack.push(this.textColor);
                hextext$applyRgbColor(instruction.getRgb(), hextext$shadow);
                break;
            case POP_COLOR:
                if (hextext$colorStack != null && !hextext$colorStack.isEmpty()) {
                    hextext$setColorFromInt(hextext$colorStack.pop());
                } else {
                    hextext$setColorFromInt(hextext$baseColor);
                }
                break;
            case RESET_TO_BASE:
                if (hextext$colorStack != null) {
                    hextext$colorStack.clear();
                }
                hextext$setColorFromInt(hextext$baseColor);
                hextext$resetFormattingStyles();
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
    private void hextext$applyVanillaColor(int colorIndex) {
        int index = Math.max(0, Math.min(colorIndex, 15));
        int paletteIndex = hextext$shadow ? index + 16 : index;
        if (colorCode != null && paletteIndex >= 0 && paletteIndex < colorCode.length) {
            hextext$setColorFromInt(colorCode[paletteIndex]);
        } else {
            hextext$setColorFromInt(hextext$baseColor);
        }
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
