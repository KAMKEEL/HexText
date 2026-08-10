package kamkeel.hextext.mixin.early.impl.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.HexText;
import kamkeel.hextext.common.util.ColorCodeUtils;
import kamkeel.hextext.common.util.GradientWrap;
import kamkeel.hextext.common.util.StringUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Chat wraps its own lines - it never calls the font renderer's wrap - so the
 * format carry and the gradient split both have to happen here, on the two
 * strings vanilla cuts the line into.
 */
@SideOnly(Side.CLIENT)
@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat extends Gui {

    @Unique
    private String hextext$wrappedFirstPart = "";

    @Unique
    private String hextext$rewrittenFirstPart;

    @ModifyVariable(method = "func_146237_a",
        at = @At(value = "STORE"),
        name = "s1")
    private String hextext$captureWrappedPrefix(String value) {
        if(HexText.getActiveProxy() == null)
            return value;

        hextext$wrappedFirstPart = value;
        return value;
    }

    /**
     * The continuation line, before its component is built. Formatting active at
     * the break is prepended so the line keeps rendering as the whole message
     * would have; a gradient still running is split rather than copied, because a
     * copied token restarts the ramp and an extracted colour flattens it - the
     * continuation opens at the colour the break was reached at, and the first
     * line is cut back to the same colour below.
     */
    @ModifyArgs(method = "func_146237_a",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ChatComponentText;<init>(Ljava/lang/String;)V", ordinal = 2))
    private void hextext$prependFormatCodes(Args args) {
        if(HexText.getActiveProxy() == null)
            return;

        String firstPart = hextext$wrappedFirstPart;
        hextext$wrappedFirstPart = "";
        hextext$rewrittenFirstPart = null;
        if (firstPart.isEmpty()) {
            return;
        }

        String continuation = args.get(0);

        // Another mod may have prepended the first line's formatting here already -
        // Hodgepodge's chat fix does - and the prefix built below says all of it
        // again. Both spellings are peeled off: the extractor's, and vanilla's own,
        // which reads §l as formatting and §#RRGGBB as nothing at all and so hands
        // back a bold that the hex after it had already cleared. Only these two
        // exact strings are removed, so codes that belong to the line survive.
        String duplicatePrefix = StringUtils.extractFormatFromString(firstPart);
        String vanillaPrefix = hextext$vanillaFormatPrefix(firstPart);
        for (int pass = 0; pass < 2; pass++) {
            if (!duplicatePrefix.isEmpty() && continuation.startsWith(duplicatePrefix)) {
                continuation = continuation.substring(duplicatePrefix.length());
            } else if (!vanillaPrefix.isEmpty() && continuation.startsWith(vanillaPrefix)) {
                continuation = continuation.substring(vanillaPrefix.length());
            } else {
                break;
            }
        }

        String prefix;
        GradientWrap.Carry carry = GradientWrap.carryAcrossBreak(firstPart, continuation);
        if (carry != null) {
            hextext$rewrittenFirstPart = carry.rewrittenFirstPart;
            String styles = StringUtils.extractFormatFromString(carry.rewrittenFirstPart);
            if (styles.startsWith(carry.rewrittenToken)) {
                styles = styles.substring(carry.rewrittenToken.length());
            }
            prefix = carry.continuationToken + styles;
        } else {
            prefix = duplicatePrefix;
        }

        if (!prefix.isEmpty()) {
            args.set(0, prefix + continuation);
        }
    }

    /** Vanilla's own formatting carry: colours latch, styles accumulate, hex is invisible to it. */
    @Unique
    private static String hextext$vanillaFormatPrefix(String text) {
        StringBuilder prefix = new StringBuilder();
        String color = "";
        for (int i = text.indexOf(167); i != -1; i = text.indexOf(167, i + 1)) {
            if (i >= text.length() - 1) {
                break;
            }
            char code = text.charAt(i + 1);
            if (ColorCodeUtils.isMinecraftColorCode(Character.toLowerCase(code))) {
                color = String.valueOf((char) 167) + code;
                prefix.setLength(0);
            } else if (ColorCodeUtils.isStyleCode(Character.toLowerCase(code))
                || ColorCodeUtils.isResetCode(Character.toLowerCase(code))) {
                prefix.append((char) 167).append(code);
            }
        }
        return color + prefix;
    }

    /** The first line again, cut back to the boundary colour the split chose. */
    @ModifyArg(method = "func_146237_a",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ChatComponentText;<init>(Ljava/lang/String;)V", ordinal = 3),
        index = 0)
    private String hextext$rewriteWrappedFirstLine(String firstLine) {
        String rewritten = hextext$rewrittenFirstPart;
        hextext$rewrittenFirstPart = null;
        return rewritten != null ? rewritten : firstLine;
    }
}
