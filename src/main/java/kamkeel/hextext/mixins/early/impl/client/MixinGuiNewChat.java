package kamkeel.hextext.mixins.early.impl.client;

import net.minecraft.client.gui.GuiNewChat;
import kamkeel.hextext.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    @Unique
    private String hextext$wrappedFormatPrefix;

    @ModifyVariable(method = "func_146237_a",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;", shift = At.Shift.AFTER),
        name = "s1")
    private String hextext$captureWrappedPrefix(String value) {
        hextext$wrappedFormatPrefix = StringUtils.extractFormatFromString(value);
        return value;
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
