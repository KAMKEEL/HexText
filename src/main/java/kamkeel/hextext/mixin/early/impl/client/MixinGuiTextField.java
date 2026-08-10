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
     * Moves the cursor one character at a time over what the editor shows.
     * <p>
     * Hodgepodge snaps the cursor past whole {@code &}-codes and deletes them as
     * units, which suits invisible codes. HexText draws them as glyphs, so this runs
     * first (last-applied HEAD injection wins) and answers the four keys with
     * vanilla behaviour.
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
     * Draws the whole visible line in one pass so codes before the cursor still
     * style what follows it.
     * <p>
     * Vanilla cuts the line at the cursor and draws it in two calls. Drawing the
     * full line from the second one left everything left of the cursor drawn twice -
     * invisible while the renderer kept submission order, but a batching one sorts
     * commands, laying one pass's shadows over the other's glyphs and stacking the
     * code wash into a block.
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
        // The caller places the cursor from this, so it must be the width of the part
        // before it, not of the line just drawn. Measured from the field's own state:
        // other mods decorate the argument with format codes, and codes have width in
        // a raw draw, so measuring the argument walked the cursor right.
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
