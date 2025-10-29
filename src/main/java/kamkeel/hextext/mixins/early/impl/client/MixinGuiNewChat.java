package kamkeel.hextext.mixins.early.impl.client;

import kamkeel.hextext.util.StringUtils;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    @Unique
    private String hextext$wrappedFormatPrefix;

    @Inject(method = "func_146237_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;", ordinal = 0, shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    private void hextext$captureWrappedPrefix(IChatComponent component, int lineId, int updateCounter, boolean refresh, CallbackInfo ci, int k, int l, net.minecraft.util.ChatComponentText chatcomponenttext, java.util.ArrayList arraylist, java.util.ArrayList arraylist1, int i1, IChatComponent ichatcomponent1, String s, int j1, net.minecraft.util.ChatComponentText chatcomponenttext1, boolean flag1, String s1) {
        hextext$wrappedFormatPrefix = StringUtils.extractFormatFromString(s1);
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
