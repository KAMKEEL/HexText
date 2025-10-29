package kamkeel.hextext.mixin.early.impl.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.client.render.FontRenderContext;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextField extends Gui {

    @Unique
    private boolean hextext$legacy$rawPushed;

    @Inject(method = "drawTextBox", at = @At("HEAD"))
    private void hextext$legacy$beginRawMode(CallbackInfo ci) {
        FontRenderContext.pushRawTextRendering();
        hextext$legacy$rawPushed = true;
    }

    @Inject(method = "drawTextBox", at = @At("RETURN"))
    private void hextext$legacy$endRawMode(CallbackInfo ci) {
        if (hextext$legacy$rawPushed) {
            FontRenderContext.popRawTextRendering();
            hextext$legacy$rawPushed = false;
        }
    }
}
