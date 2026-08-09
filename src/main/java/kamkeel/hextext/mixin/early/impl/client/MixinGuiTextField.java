package kamkeel.hextext.mixin.early.impl.client;

import com.llamalad7.mixinextras.sugar.Local;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.render.FontRenderContext;
import kamkeel.hextext.common.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextField extends Gui {

    @Unique
    private String hextext$lastRawText = "";

    @Unique
    private boolean hextext$rawDraw;

    @Inject(method = "drawTextBox", at = @At("HEAD"))
    private void hextext$legacy$resetRawText(CallbackInfo ci) {
        hextext$lastRawText = "";
        hextext$rawDraw = HexText.getActiveProxy() != null && HexText.rawRenderingEnabled;
    }

    @Inject(method = "drawTextBox", at = @At("RETURN"))
    private void hextext$legacy$endRawMode(CallbackInfo ci) {
        if (HexText.getActiveProxy() == null)
            return;

        if (HexText.rawRenderingEnabled) {
            FontRenderContext.popRawTextRendering();
            HexText.rawRenderingEnabled = false;
        }

        hextext$lastRawText = "";
    }

    @Inject(method = "setSelectionPos", at = @At("HEAD"))
    private void hextext$beginRawSelection(int pos, CallbackInfo ci) {
        if (HexText.getActiveProxy() == null)
            return;

        // Only if chat raw-rendering is actually active by config
        if (!HexText.getActiveProxy().convertAmpersandsInChat()
            && !HexText.getActiveProxy().allowUniversalAmpersand())
            return;

        FontRenderContext.pushRawTextRendering();
    }

    @Inject(method = "setSelectionPos", at = @At("RETURN"))
    private void hextext$endRawSelection(int pos, CallbackInfo ci) {
        if (HexText.getActiveProxy() == null)
            return;

        FontRenderContext.popRawTextRendering();
    }

    @Redirect(
        method = "drawTextBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;trimStringToWidth(Ljava/lang/String;I)Ljava/lang/String;"
        )
    )
    private String hextext$captureRawText(FontRenderer fr, String input, int width) {
        String result = fr.trimStringToWidth(input, width);
        hextext$lastRawText = result;
        return result;
    }

    /**
     * In raw mode the whole visible line goes down in one pass, so codes sitting
     * before the cursor still style what comes after it.
     *
     * <p>Vanilla cuts the line at the cursor and draws it in two calls, and the
     * full line used to be drawn from the second one - with the first still
     * drawing the part before the cursor, so everything to its left went down
     * twice. Two passes of the same glyphs read as one while the font renderer
     * kept submission order, but a batching one sorts the commands, which lays
     * the second pass's shadows over the first pass's glyphs and stacks the wash
     * behind a code until it turns into a block. The doubled run grew as the
     * cursor moved right, and vanished at either end of the line where only one
     * of the two calls runs at all.</p>
     */
    @Redirect(
        method = "drawTextBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I",
            ordinal = 0 // first call: the text before the cursor
        )
    )
    private int hextext$drawFullVisibleInRaw(FontRenderer fontRenderer, String prefix, int x, int y, int color) {
        if (!hextext$rawDraw) {
            return fontRenderer.drawStringWithShadow(prefix, x, y, color);
        }

        fontRenderer.drawStringWithShadow(hextext$lastRawText, x, y, color);
        // The caller places the cursor from what this returns, so it has to stay the
        // width of the part in front of it and not of the line that was just drawn.
        return x + fontRenderer.getStringWidth(prefix);
    }

    /** The line is already down in full, so the second call has nothing left to draw. */
    @Redirect(
        method = "drawTextBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I",
            ordinal = 1 // second call: s.substring(j)
        )
    )
    private int hextext$skipSuffixInRaw(FontRenderer fontRenderer, String suffix, int x, int y, int color) {
        if (!hextext$rawDraw) {
            return fontRenderer.drawStringWithShadow(suffix, x, y, color);
        }
        return x;
    }
}
