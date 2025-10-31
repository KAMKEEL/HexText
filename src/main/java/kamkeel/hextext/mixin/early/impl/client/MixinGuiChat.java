package kamkeel.hextext.mixin.early.impl.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.render.FontRenderContext;
import kamkeel.hextext.common.util.StringUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends Gui {

    @Inject(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiTextField;drawTextBox()V", shift = At.Shift.BEFORE))
    private void hextext$PushRawRendering(CallbackInfo ci) {
        if(HexText.getActiveProxy() == null)
            return;

        if(!HexText.getActiveProxy().convertAmpersandsInChat() && !HexText.getActiveProxy().allowUniversalAmpersand())
            return;

        FontRenderContext.pushRawTextRendering();
        HexText.rawRenderingEnabled = true;
    }

    @ModifyArg(
        method = "keyTyped",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;func_71165_d(Ljava/lang/String;)V")
    )
    private String hextext$convertSubmittedChat(String original) {
        if (HexText.getActiveProxy() == null)
            return original;

        if (!HexText.getActiveProxy().convertAmpersandsInChat())
            return original;

        return StringUtils.convertAmpersandsToSectionSigns(original);
    }
}
