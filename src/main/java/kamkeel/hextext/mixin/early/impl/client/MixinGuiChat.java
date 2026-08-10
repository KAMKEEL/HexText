package kamkeel.hextext.mixin.early.impl.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.compat.AngelicaTextTranslator;
import kamkeel.hextext.client.render.FontRenderContext;
import kamkeel.hextext.common.compat.AngelicaCompatibility;
import kamkeel.hextext.common.util.StringUtils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen {

    @Inject(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiTextField;drawTextBox()V", shift = At.Shift.BEFORE))
    private void hextext$PushRawRendering(CallbackInfo ci) {
        if(HexText.getActiveProxy() == null)
            return;

        if(!HexText.getActiveProxy().convertAmpersandsInChat() && !HexText.getActiveProxy().allowUniversalAmpersand())
            return;

        FontRenderContext.pushRawTextRendering();
        HexText.rawRenderingEnabled = true;
    }

    @ModifyArg(method = "func_146403_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;sendChatMessage(Ljava/lang/String;)V"))
    public String hextext$ConvertChatOnSubmit(String original)
    {
        if (HexText.getActiveProxy() == null)
            return original;

        if (!HexText.getActiveProxy().convertAmpersandsInChat())
            return original;

        String converted = StringUtils.convertAmpersandsToSectionSigns(original);
        // Angelica's own letters are not HexText codes and the conversion above
        // leaves them as typed; where its renderer is the one drawing, they are
        // spelled out too, so the sent line keeps the rainbow the preview showed.
        if (AngelicaCompatibility.isAngelicaFontRendererActive()) {
            converted = AngelicaTextTranslator.convertAngelicaSendCodes(converted);
        }
        return converted;
    }
}
