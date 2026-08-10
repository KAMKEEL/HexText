package kamkeel.hextext.mixin.early.impl.client;

import com.llamalad7.mixinextras.sugar.Local;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.render.FontRenderContext;
import kamkeel.hextext.common.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextField extends Gui {

    @Unique
    private String hextext$lastRawText = "";

    @Unique
    private boolean hextext$rawDraw;

    @Shadow
    private int lineScrollOffset;

    @Shadow
    private int cursorPosition;

    @Shadow
    private boolean isFocused;

    @Shadow
    private boolean isEnabled;

    @Shadow
    private String text;

    @Inject(method = "drawTextBox", at = @At("HEAD"))
    private void hextext$legacy$resetRawText(CallbackInfo ci) {
        hextext$lastRawText = "";
        hextext$rawDraw = HexText.getActiveProxy() != null && HexText.rawRenderingEnabled;
    }

    @Inject(method = "drawTextBox", at = @At("RETURN"))
    private void hextext$legacy$endRawMode(CallbackInfo ci) {
        if (HexText.getActiveProxy() == null)
            return;

        if (HexText.rawRenderingEnabled) {
            FontRenderContext.popRawTextRendering();
            HexText.rawRenderingEnabled = false;
        }

        hextext$lastRawText = "";
    }

    /**
     * Keeps the cursor moving over what the editor shows, one character at a time.
     *
     * <p>Hodgepodge snaps the cursor past whole {@code &}-codes and deletes them as
     * units, which reads naturally when the codes are invisible at render time. In
     * a HexText editor they are visible glyphs, and a cursor that leaps eight of
     * them at once is jumping over characters the person editing can see. This
     * runs ahead of that handler - the last-applied injection at HEAD executes
     * first - and answers the four keys it would have claimed with vanilla's own
     * behaviour, so editing here works exactly as it does without Hodgepodge.</p>
     */
    @Inject(method = "textboxKeyTyped", at = @At("HEAD"), cancellable = true)
    private void hextext$vanillaCursorKeys(char typedChar, int keyCode, CallbackInfoReturnable<Boolean> cir) {
        if (HexText.getActiveProxy() == null || !this.isFocused) {
            return;
        }
        if (!HexText.getActiveProxy().convertAmpersandsInChat()
            && !HexText.getActiveProxy().allowUniversalAmpersand()) {
            return;
        }
        if (this.text == null || this.text.indexOf('&') == -1) {
            return;
        }
        if (GuiScreen.isCtrlKeyDown() && keyCode != 14 && keyCode != 211) {
            // Word-wise movement is not char-wise movement; vanilla handles it the
            // same way either side of this, so there is nothing to protect.
            return;
        }

        GuiTextField self = (GuiTextField) (Object) this;
        switch (keyCode) {
            case 203: // LEFT
                if (GuiScreen.isShiftKeyDown()) {
                    self.setSelectionPos(self.getSelectionEnd() - 1);
                } else {
                    self.moveCursorBy(-1);
                }
                cir.setReturnValue(true);
                break;
            case 205: // RIGHT
                if (GuiScreen.isShiftKeyDown()) {
                    self.setSelectionPos(self.getSelectionEnd() + 1);
                } else {
                    self.moveCursorBy(1);
                }
                cir.setReturnValue(true);
                break;
            case 14: // BACKSPACE
                if (this.isEnabled) {
                    if (GuiScreen.isCtrlKeyDown()) {
                        self.deleteWords(-1);
                    } else {
                        self.deleteFromCursor(-1);
                    }
                }
                cir.setReturnValue(true);
                break;
            case 211: // DELETE
                if (this.isEnabled) {
                    if (GuiScreen.isCtrlKeyDown()) {
                        self.deleteWords(1);
                    } else {
                        self.deleteFromCursor(1);
                    }
                }
                cir.setReturnValue(true);
                break;
            default:
                break;
        }
    }

    @Inject(method = "setSelectionPos", at = @At("HEAD"))
    private void hextext$beginRawSelection(int pos, CallbackInfo ci) {
        if (HexText.getActiveProxy() == null)
            return;

        // Only if chat raw-rendering is actually active by config
        if (!HexText.getActiveProxy().convertAmpersandsInChat()
            && !HexText.getActiveProxy().allowUniversalAmpersand())
            return;

        FontRenderContext.pushRawTextRendering();
    }

    @Inject(method = "setSelectionPos", at = @At("RETURN"))
    private void hextext$endRawSelection(int pos, CallbackInfo ci) {
        if (HexText.getActiveProxy() == null)
            return;

        FontRenderContext.popRawTextRendering();
    }

    @Redirect(
        method = "drawTextBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;trimStringToWidth(Ljava/lang/String;I)Ljava/lang/String;"
        )
    )
    private String hextext$captureRawText(FontRenderer fr, String input, int width) {
        String result = fr.trimStringToWidth(input, width);
        hextext$lastRawText = result;
        return result;
    }

    /**
     * In raw mode the whole visible line goes down in one pass, so codes sitting
     * before the cursor still style what comes after it.
     *
     * <p>Vanilla cuts the line at the cursor and draws it in two calls, and the
     * full line used to be drawn from the second one - with the first still
     * drawing the part before the cursor, so everything to its left went down
     * twice. Two passes of the same glyphs read as one while the font renderer
     * kept submission order, but a batching one sorts the commands, which lays
     * the second pass's shadows over the first pass's glyphs and stacks the wash
     * behind a code until it turns into a block. The doubled run grew as the
     * cursor moved right, and vanished at either end of the line where only one
     * of the two calls runs at all.</p>
     */
    @Redirect(
        method = "drawTextBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I",
            ordinal = 0 // first call: the text before the cursor
        )
    )
    private int hextext$drawFullVisibleInRaw(FontRenderer fontRenderer, String prefix, int x, int y, int color) {
        if (!hextext$rawDraw) {
            return fontRenderer.drawStringWithShadow(prefix, x, y, color);
        }

        fontRenderer.drawStringWithShadow(hextext$lastRawText, x, y, color);
        // The caller places the cursor from what this returns, so it has to stay the
        // width of the part in front of it and not of the line that was just drawn.
        // Measured from the field's own state, not the argument: other mods decorate
        // the argument with format codes before it arrives - Hodgepodge prepends §r
        // and the scrolled-past formatting - and in a raw draw codes have width, so
        // measuring the decorated string walked the cursor right of where it was.
        final int prefixLength = Math.max(0,
            Math.min(this.cursorPosition - this.lineScrollOffset, hextext$lastRawText.length()));
        return x + fontRenderer.getStringWidth(hextext$lastRawText.substring(0, prefixLength));
    }

    /** The line is already down in full, so the second call has nothing left to draw. */
    @Redirect(
        method = "drawTextBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I",
            ordinal = 1 // second call: s.substring(j)
        )
    )
    private int hextext$skipSuffixInRaw(FontRenderer fontRenderer, String suffix, int x, int y, int color) {
        if (!hextext$rawDraw) {
            return fontRenderer.drawStringWithShadow(suffix, x, y, color);
        }
        return x;
    }
}
