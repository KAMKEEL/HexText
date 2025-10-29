package kamkeel.hextext.mixin.client;

import kamkeel.hextext.common.util.StringUtils;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GuiNewChat.class)
public abstract class GuiNewChatMixin {

    @Unique
    private String hextext$wrappedFormatPrefix = "";

    @ModifyVariable(method = "func_146237_a",
        at = @At(value = "STORE"),
        name = "s1")
    private String hextext$captureWrappedPrefix(String value) {
        hextext$wrappedFormatPrefix = StringUtils.extractFormatFromString(value);
        return value;
    }

    @ModifyArgs(method = "func_146237_a",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ChatComponentText;<init>(Ljava/lang/String;)V", ordinal = 2))
    private void hextext$prependFormatCodes(Args args) {
        if (!hextext$wrappedFormatPrefix.isEmpty()) {
            String value = args.get(0);
            args.set(0, hextext$wrappedFormatPrefix + value);
        }
        hextext$wrappedFormatPrefix = "";
    }
}
