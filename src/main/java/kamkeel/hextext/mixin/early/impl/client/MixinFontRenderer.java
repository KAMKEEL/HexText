package kamkeel.hextext.mixin.early.impl.client;

import kamkeel.hextext.HexText;
import kamkeel.hextext.client.render.font.FontRendererBridge;
import kamkeel.hextext.client.render.font.FontRendererRenderPipeline;
import kamkeel.hextext.client.render.font.GlyphRenderer;
import kamkeel.hextext.common.util.StringUtils;
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

@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer implements FontRendererBridge {

    @Shadow
    private boolean randomStyle;
    @Shadow
    private boolean boldStyle;
    @Shadow
    private boolean strikethroughStyle;
    @Shadow
    private boolean underlineStyle;
    @Shadow
    private boolean italicStyle;
    @Shadow
    private int textColor;
    @Shadow
    private float alpha;
    @Shadow
    private float red;
    @Shadow
    private float blue;
    @Shadow
    private float green;
    @Shadow
    protected float posX;
    @Shadow
    protected float posY;
    @Shadow
    public int FONT_HEIGHT;
    @Shadow
    private int[] colorCode;

    @Shadow
    protected abstract void setColor(float r, float g, float b, float a);

    @Shadow
    protected abstract float renderDefaultChar(int a, boolean b);

    @Shadow
    protected abstract float renderUnicodeChar(char a, boolean b);

    @Invoker("renderDefaultChar")
    protected abstract float hextext$invokeRenderDefaultChar(int character, boolean italic);

    @Invoker("renderUnicodeChar")
    protected abstract float hextext$invokeRenderUnicodeChar(char character, boolean italic);

    @Unique
    private final FontRendererRenderPipeline hextext$pipeline = new FontRendererRenderPipeline(this);

    @Unique
    private final GlyphRenderer hextext$glyphRenderer = new GlyphRenderer() {
        @Override
        public float renderDefault(int glyphIndex, boolean italicFlag) {
            return hextext$invokeRenderDefaultChar(glyphIndex, italicFlag);
        }

        @Override
        public float renderUnicode(char character, boolean italicFlag) {
            return hextext$invokeRenderUnicodeChar(character, italicFlag);
        }
    };

    @Inject(method = "renderString", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;setColor(FFFF)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    private void hextext$capturePreparedColor(String text, int x, int y, int color, boolean dropShadow,
                                              CallbackInfoReturnable<Integer> cir) {
        if(HexText.getActiveProxy() == null)
            return;

        hextext$pipeline.capturePreparedColor(color);
    }

    @Inject(method = "renderStringAtPos", at = @At("HEAD"))
    private void hextext$begin(String text, boolean shadow, CallbackInfo ci) {
        if(HexText.getActiveProxy() == null)
            return;

        hextext$pipeline.begin(text, shadow);
    }

    @ModifyVariable(method = "renderStringAtPos", at = @At("HEAD"), argsOnly = true)
    private String hextext$replaceRenderText(String text) {
        if(HexText.getActiveProxy() == null)
            return text;

        return hextext$pipeline.adjustRenderText(text);
    }

    @Inject(method = "renderStringAtPos", at = @At(value = "INVOKE_ASSIGN", target = "Ljava/lang/String;charAt(I)C"),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private void hextext$applyInstructions(String text, boolean shadow, CallbackInfo ci, int index, char current) {
        if(HexText.getActiveProxy() == null)
            return;

        hextext$pipeline.applyInstructions(text, index, current);
    }

    @Inject(method = "renderStringAtPos", at = @At("TAIL"))
    private void hextext$end(String text, boolean shadow, CallbackInfo ci) {
        if(HexText.getActiveProxy() == null)
            return;

        hextext$pipeline.end();
    }

    @Inject(method = "getStringWidth", at = @At("RETURN"), cancellable = true)
    private void hextext$width(String text, CallbackInfoReturnable<Integer> cir) {
        if(HexText.getActiveProxy() == null)
            return;

        cir.setReturnValue(hextext$pipeline.computeStringWidth(text));
    }

    @Inject(method = "sizeStringToWidth", at = @At("RETURN"), cancellable = true)
    private void hextext$size(String text, int maxWidth, CallbackInfoReturnable<Integer> cir) {
        if(HexText.getActiveProxy() == null)
            return;

        cir.setReturnValue(hextext$pipeline.computeLineBreakIndex(text, maxWidth));
    }

    @Inject(method = "trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void hextext$trim(String text, int width, boolean reverse, CallbackInfoReturnable<String> cir) {
        if(HexText.getActiveProxy() == null)
            return;

        cir.setReturnValue(hextext$pipeline.trimStringToWidth(text, width, reverse));
    }

    @Inject(method = "wrapFormattedStringToWidth", at = @At("RETURN"), cancellable = true)
    private void hextext$wrap(String text, int width, CallbackInfoReturnable<String> cir) {
        if(HexText.getActiveProxy() == null)
            return;

        cir.setReturnValue(hextext$pipeline.wrapFormattedString(text, width));
    }

    @Inject(method = "getFormatFromString", at = @At("RETURN"), cancellable = true)
    private static void hextext$format(String text, CallbackInfoReturnable<String> cir) {
        if(HexText.getActiveProxy() == null)
            return;

        cir.setReturnValue(StringUtils.extractFormatFromString(text));
    }

    @Redirect(method = "renderCharAtPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;renderDefaultChar(IZ)F"))
    private float hextext$renderDefaultChar(FontRenderer fontRenderer, int character, boolean italic,
                                            int glyphIndex, char glyph, boolean italicFlag) {
        if(HexText.getActiveProxy() == null)
            return  this.renderDefaultChar(glyphIndex, italicFlag);

        return hextext$pipeline.renderGlyph(glyph, italic, false, character, glyph, hextext$glyphRenderer);
    }

    @Redirect(method = "renderCharAtPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;renderUnicodeChar(CZ)F"))
    private float hextext$renderUnicodeChar(FontRenderer fontRenderer, char character, boolean italic,
                                            int glyphIndex, char glyph, boolean italicFlag) {
        if(HexText.getActiveProxy() == null)
            return this.renderUnicodeChar(glyph, italicFlag);

        return hextext$pipeline.renderGlyph(glyph, italic, true, 0, character, hextext$glyphRenderer);
    }

    @Inject(method = "doDraw", at = @At("TAIL"), remap = false)
    private void hextext$advanceVisibleGlyphIndex(float width, CallbackInfo ci) {
        if(HexText.getActiveProxy() == null)
            return;

        hextext$pipeline.advanceGlyphIndex();
    }

    @Override
    @Unique
    public FontRenderer getFontRenderer() {
        return (FontRenderer) (Object) this;
    }

    @Override
    @Unique
    public void setRandomStyle(boolean enabled) {
        this.randomStyle = enabled;
    }

    @Override
    @Unique
    public void setBoldStyle(boolean enabled) {
        this.boldStyle = enabled;
    }

    @Override
    @Unique
    public void setStrikethroughStyle(boolean enabled) {
        this.strikethroughStyle = enabled;
    }

    @Override
    @Unique
    public void setUnderlineStyle(boolean enabled) {
        this.underlineStyle = enabled;
    }

    @Override
    @Unique
    public void setItalicStyle(boolean enabled) {
        this.italicStyle = enabled;
    }

    @Override
    @Unique
    public void setTextColor(int color) {
        this.textColor = color;
    }

    @Override
    @Unique
    public int getTextColor() {
        return this.textColor;
    }

    @Override
    @Unique
    public float getAlpha() {
        return this.alpha;
    }

    @Override
    @Unique
    public float getRedComponent() {
        return this.red;
    }

    @Override
    @Unique
    public float getBlueComponent() {
        return this.blue;
    }

    @Override
    @Unique
    public float getGreenComponent() {
        return this.green;
    }

    @Override
    @Unique
    public int[] getColorCodePalette() {
        return this.colorCode;
    }

    @Override
    @Unique
    public float getPosX() {
        return this.posX;
    }

    @Override
    @Unique
    public float getPosY() {
        return this.posY;
    }

    @Override
    @Unique
    public int getFontHeight() {
        return this.FONT_HEIGHT;
    }

    @Override
    @Unique
    public void applyColorComponents(float r, float g, float b, float a) {
        setColor(r, g, b, a);
    }

    @Override
    @Unique
    public void resetFormattingStyles() {
        this.randomStyle = false;
        this.boldStyle = false;
        this.strikethroughStyle = false;
        this.underlineStyle = false;
        this.italicStyle = false;
    }
}
