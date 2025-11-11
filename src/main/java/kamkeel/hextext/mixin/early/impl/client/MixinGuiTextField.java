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

    @Inject(method = "drawTextBox", at = @At("RETURN"))
    private void hextext$legacy$endRawMode(CallbackInfo ci) {
        if(HexText.getActiveProxy() == null)
            return;

        if (HexText.rawRenderingEnabled) {
            FontRenderContext.popRawTextRendering();
            HexText.rawRenderingEnabled = false;
        }
    }
}
