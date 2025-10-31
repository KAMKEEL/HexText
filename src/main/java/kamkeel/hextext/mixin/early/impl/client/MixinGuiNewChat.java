package kamkeel.hextext.mixin.early.impl.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.HexText;
import kamkeel.hextext.common.util.StringUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@SideOnly(Side.CLIENT)
@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat extends Gui {

    @Unique
    private String hextext$wrappedFormatPrefix = "";

    @ModifyVariable(method = "func_146237_a",
        at = @At(value = "STORE"),
        name = "s1")
    private String hextext$captureWrappedPrefix(String value) {
        if(HexText.getActiveProxy() == null)
            return value;

        hextext$wrappedFormatPrefix = StringUtils.extractFormatFromString(value);
        return value;
    }

    @ModifyArgs(method = "func_146237_a",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ChatComponentText;<init>(Ljava/lang/String;)V", ordinal = 2))
    private void hextext$prependFormatCodes(Args args) {
        if(HexText.getActiveProxy() == null)
            return;

        if (!hextext$wrappedFormatPrefix.isEmpty()) {
            String value = args.get(0);
            args.set(0, hextext$wrappedFormatPrefix + value);
        }
        hextext$wrappedFormatPrefix = "";
    }
}
