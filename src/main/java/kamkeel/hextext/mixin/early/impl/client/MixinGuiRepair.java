package kamkeel.hextext.mixin.early.impl.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.render.FontRenderContext;
import kamkeel.hextext.common.util.StringUtils;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(GuiRepair.class)
public abstract class MixinGuiRepair extends GuiContainer {

    public MixinGuiRepair(Container p_i1072_1_) {
        super(p_i1072_1_);
    }

    @Inject(
        method = "drawScreen",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiTextField;drawTextBox()V",
            shift = At.Shift.BEFORE
        )
    )
    private void hextext$enableRawRendering(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (HexText.getActiveProxy() == null) {
            return;
        }

        if (!HexText.getActiveProxy().convertAmpersandsInRepairs()) {
            return;
        }

        FontRenderContext.pushRawTextRendering();
        HexText.rawRenderingEnabled = true;
    }

    @ModifyArg(method = "sendSlotContents", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiTextField;setText(Ljava/lang/String;)V"))
    public String hextext$setTextToAmpersands(String original)
    {
        if (HexText.getActiveProxy() == null)
            return original;

        if (!HexText.getActiveProxy().convertAmpersandsInRepairs())
            return original;

        return StringUtils.convertSectionSignsToAmpersands(original);
    }
}
