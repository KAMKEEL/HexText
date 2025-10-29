package kamkeel.hextext.mixins.early.impl.client;

import net.minecraft.client.gui.GuiNewChat;
import kamkeel.hextext.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    @Unique
    private String hextext$wrappedFormatPrefix;

    @Redirect(method = "func_146237_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getFormatFromString(Ljava/lang/String;)Ljava/lang/String;"))
    private String hextext$captureWrappedPrefix(String original) {
        hextext$wrappedFormatPrefix = StringUtils.extractFormatFromString(original);
        return hextext$wrappedFormatPrefix;
    }

    @ModifyVariable(method = "func_146237_a",
        at = @At(value = "NEW", target = "net/minecraft/util/ChatComponentText", ordinal = 2, shift = At.Shift.BEFORE),
        name = "s2")
    private String hextext$prependFormatCodes(String value) {
        if (hextext$wrappedFormatPrefix != null && !hextext$wrappedFormatPrefix.isEmpty()) {
            value = hextext$wrappedFormatPrefix + value;
        }
        hextext$wrappedFormatPrefix = "";
        return value;
    }
}
