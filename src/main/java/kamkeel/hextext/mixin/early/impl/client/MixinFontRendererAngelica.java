package kamkeel.hextext.mixin.early.impl.client;

import kamkeel.hextext.HexText;
import kamkeel.hextext.client.compat.AngelicaTextTranslator;
import kamkeel.hextext.client.render.FontRenderContext;
import kamkeel.hextext.client.render.FontRendererUtils;
import kamkeel.hextext.common.util.StringUtils;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces {@link MixinFontRenderer} when Angelica's batching renderer owns drawing. Vanilla's
 * glyph loop never runs there, so tokens are rewritten into Angelica's grammar instead.
 * <p>
 * Only draw methods translate. Callers use measurement results as indices into the string they
 * passed, and a translated string has different lengths, so those are answered with HexText's
 * own metrics over the original text. Priority stays below Angelica's 1000 so the rewrite lands
 * ahead of its hooks.
 */
@Mixin(value = FontRenderer.class, priority = 500)
public abstract class MixinFontRendererAngelica {

    @ModifyVariable(method = "drawString(Ljava/lang/String;IIIZ)I", at = @At("HEAD"), argsOnly = true)
    private String hextext$translateDrawString(String text) {
        return AngelicaTextTranslator.translate(text);
    }

    @ModifyVariable(method = "renderString", at = @At("HEAD"), argsOnly = true)
    private String hextext$translateRenderString(String text) {
        return AngelicaTextTranslator.translate(text);
    }

    @Inject(method = "getStringWidth", at = @At("RETURN"), cancellable = true)
    private void hextext$width(String text, CallbackInfoReturnable<Integer> cir) {
        if (HexText.getActiveProxy() == null) {
            return;
        }
        cir.setReturnValue(Math.round(FontRendererUtils.calculateMaxLineWidth(
            (FontRenderer) (Object) this, text, FontRenderContext.isRawTextRendering())));
    }

    @Inject(method = "sizeStringToWidth", at = @At("RETURN"), cancellable = true)
    private void hextext$size(String text, int maxWidth, CallbackInfoReturnable<Integer> cir) {
        if (HexText.getActiveProxy() == null) {
            return;
        }
        cir.setReturnValue(FontRendererUtils.computeLineBreakIndex(
            (FontRenderer) (Object) this, text, maxWidth, FontRenderContext.isRawTextRendering()));
    }

    @Inject(method = "trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void hextext$trim(String text, int width, boolean reverse, CallbackInfoReturnable<String> cir) {
        if (HexText.getActiveProxy() == null || text == null || text.isEmpty()) {
            return;
        }
        boolean rawMode = FontRenderContext.isRawTextRendering();
        FontRenderer self = (FontRenderer) (Object) this;
        if (!reverse) {
            int endIndex = FontRendererUtils.computeTrimIndex(self, text, width, rawMode);
            cir.setReturnValue(text.substring(0, Math.min(endIndex, text.length())));
        } else {
            cir.setReturnValue(FontRendererUtils.trimStringFromEnd(self, text, width, rawMode));
        }
    }

    @Inject(method = "wrapFormattedStringToWidth", at = @At("RETURN"), cancellable = true)
    private void hextext$wrap(String text, int width, CallbackInfoReturnable<String> cir) {
        if (HexText.getActiveProxy() == null) {
            return;
        }
        cir.setReturnValue(FontRendererUtils.wrapFormattedString(
            (FontRenderer) (Object) this, text, width, FontRenderContext.isRawTextRendering()));
    }

    /** The formatting callers carry onto a continuation line; the chat mixin peels off this same string. */
    @Inject(method = "getFormatFromString", at = @At("RETURN"), cancellable = true)
    private static void hextext$format(String text, CallbackInfoReturnable<String> cir) {
        if (HexText.getActiveProxy() == null) {
            return;
        }
        cir.setReturnValue(StringUtils.extractFormatFromString(text));
    }
}
