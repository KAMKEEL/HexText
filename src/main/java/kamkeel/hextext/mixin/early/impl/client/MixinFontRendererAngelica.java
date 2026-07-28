package kamkeel.hextext.mixin.early.impl.client;

import kamkeel.hextext.client.compat.AngelicaTextTranslator;
import kamkeel.hextext.client.render.FontRenderContext;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Lightweight replacement for {@link MixinFontRenderer} installed when Angelica's batching font
 * renderer owns string drawing. Vanilla's renderStringAtPos never runs in that configuration, so
 * instead of hooking the glyph pipeline this mixin rewrites HexText tokens into the section-sign
 * grammar Angelica understands before Angelica's own hooks capture the text.
 *
 * <p>The priority must stay below Angelica's default of 1000: injections at HEAD execute in
 * mixin application order, so applying first places the rewrite ahead of Angelica's cancellable
 * drawString/renderString injections and its ampersand conversion on the measurement methods.
 * Verified against the transformed class dump - Angelica's handlers receive the rewritten text.
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

    @ModifyVariable(method = "getStringWidth", at = @At("HEAD"), argsOnly = true)
    private String hextext$translateWidth(String text) {
        return AngelicaTextTranslator.translate(text);
    }

    @ModifyVariable(method = "sizeStringToWidth", at = @At("HEAD"), argsOnly = true)
    private String hextext$translateSize(String text) {
        return AngelicaTextTranslator.translate(text);
    }

    /**
     * Trimming is the one hook whose <em>result</em> is drawn rather than measured.
     *
     * <p>{@code MixinGuiTextField} keeps what this returns and hands it back to
     * {@code drawStringWithShadow}, so translating here means translating twice: the
     * second pass sees the section-x token the first one emitted, spells it out as a
     * literal, and emits another beside it. That is the doubling on screen.</p>
     *
     * <p>Left alone while raw, the field keeps the characters the reader typed and the
     * draw translates them once. Nothing is lost by measuring the untranslated string,
     * because everything translation adds is a formatting token and those are zero
     * width - the cut lands in the same place either way.</p>
     */
    @ModifyVariable(method = "trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("HEAD"), argsOnly = true)
    private String hextext$translateTrim(String text) {
        if (FontRenderContext.isRawTextRendering()) {
            return text;
        }
        return AngelicaTextTranslator.translate(text);
    }

    @ModifyVariable(method = "listFormattedStringToWidth", at = @At("HEAD"), argsOnly = true)
    private String hextext$translateListWrap(String text) {
        return AngelicaTextTranslator.translate(text);
    }

    @ModifyVariable(method = "wrapFormattedStringToWidth", at = @At("HEAD"), argsOnly = true)
    private String hextext$translateWrap(String text) {
        return AngelicaTextTranslator.translate(text);
    }

    @ModifyVariable(method = "getFormatFromString", at = @At("HEAD"), argsOnly = true)
    private static String hextext$translateFormat(String text) {
        return AngelicaTextTranslator.translate(text);
    }
}
