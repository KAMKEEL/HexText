package kamkeel.hextext.mixin.early.impl.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.render.FontRenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(targets = "net.minecraft.client.gui.inventory.GuiRepair")
public abstract class MixinGuiRepair {

    @Inject(
        method = "drawGuiContainerForegroundLayer(II)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiTextField;drawTextBox()V",
            shift = At.Shift.BEFORE
        )
    )
    private void hextext$enableRawRendering(int mouseX, int mouseY, CallbackInfo ci) {
        if (HexText.getActiveProxy() == null) {
            return;
        }

        if (!HexText.getActiveProxy().convertAmpersandsInRepairs()
            && !HexText.getActiveProxy().allowUniversalAmpersand()) {
            return;
        }

        FontRenderContext.pushRawTextRendering();
        HexText.rawRenderingEnabled = true;
    }
}
