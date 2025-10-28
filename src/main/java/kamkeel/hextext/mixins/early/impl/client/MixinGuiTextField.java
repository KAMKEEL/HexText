package kamkeel.hextext.mixins.early.impl.client;

import kamkeel.hextext.client.LegacyFontRenderContext;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiTextField.class)
public abstract class MixinGuiTextField {

    @Unique
    private boolean angelica$legacy$rawPushed;

    @Inject(method = "drawTextBox", at = @At("HEAD"))
    private void angelica$legacy$beginRawMode(CallbackInfo ci) {
        LegacyFontRenderContext.pushRawTextRendering();
        angelica$legacy$rawPushed = true;
    }

    @Inject(method = "drawTextBox", at = @At("RETURN"))
    private void angelica$legacy$endRawMode(CallbackInfo ci) {
        if (angelica$legacy$rawPushed) {
            LegacyFontRenderContext.popRawTextRendering();
            angelica$legacy$rawPushed = false;
        }
    }
}
