package kamkeel.hextext.mixin.early.impl.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.render.FontRenderContext;
import kamkeel.hextext.common.util.StringUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextField extends Gui {

    @Unique
    private boolean hextext$legacy$rawPushed;

    @Inject(method = "drawTextBox", at = @At("HEAD"))
    private void hextext$legacy$beginRawMode(CallbackInfo ci) {
        if(HexText.getActiveProxy() == null)
            return;

        FontRenderContext.pushRawTextRendering();
        hextext$legacy$rawPushed = true;
    }

    @Inject(method = "drawTextBox", at = @At("RETURN"))
    private void hextext$legacy$endRawMode(CallbackInfo ci) {
        if(HexText.getActiveProxy() == null)
            return;

        if (hextext$legacy$rawPushed) {
            FontRenderContext.popRawTextRendering();
            hextext$legacy$rawPushed = false;
        }
    }

    @Inject(method = "getText", at = @At("RETURN"), cancellable = true)
    private void hextext$legacy$endRawMode(CallbackInfoReturnable<String> cir) {
        if(!HexText.getActiveProxy().allowAmpersand())
            return;

        cir.setReturnValue(StringUtils.convertAmpersandsToSectionSigns(cir.getReturnValue()));
    }
}
