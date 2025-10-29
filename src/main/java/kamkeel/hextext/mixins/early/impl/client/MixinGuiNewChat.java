package kamkeel.hextext.mixins.early.impl.client;

import java.util.ArrayList;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import kamkeel.hextext.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat {

    @SuppressWarnings("unchecked")
    @ModifyVariable(method = "func_146237_a",
        at = @At(value = "NEW", target = "net/minecraft/util/ChatComponentText", ordinal = 2, shift = At.Shift.BEFORE),
        name = "s2")
    private String hextext$prependFormatCodes(String value, IChatComponent component, int id, int updateCounter,
            boolean refresh, int maxWidth, int currentWidth, ChatComponentText lineBuilder,
            ArrayList<ChatComponentText> wrappedLines, ArrayList<IChatComponent> remaining, int componentIndex,
            IChatComponent child, String rendered, int renderedWidth, ChatComponentText renderedPart,
            boolean trimmed) {
        String prefix = StringUtils.extractFormatFromString(renderedPart.getChatComponentText_TextValue());
        if (!prefix.isEmpty()) {
            return prefix + value;
        }
        return value;
    }
}
