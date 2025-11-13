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

    @Inject(method = "drawTextBox", at = @At("HEAD"))
    private void hextext$legacy$resetRawText(CallbackInfo ci) {
        hextext$lastRawText = "";
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
     * In raw chat mode, when GuiTextField draws the suffix (text after cursor),
     * instead draw the full visible substring `s` so earlier colour codes still
     * affect the text after the cursor.
     */
    @Redirect(
        method = "drawTextBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I",
            ordinal = 1 // second call: s.substring(j)
        )
    )
    private int hextext$drawFullVisibleInRaw(FontRenderer fontRenderer, String suffix, int x, int y, int color) {
        GuiTextField self = (GuiTextField) (Object) this;

        int l = self.getEnableBackgroundDrawing()
            ? self.xPosition + 4
            : self.xPosition;

        fontRenderer.drawStringWithShadow(hextext$lastRawText, l, y, color);
        return x;
    }
}
