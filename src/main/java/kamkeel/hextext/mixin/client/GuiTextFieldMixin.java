package kamkeel.hextext.mixin.client;

import kamkeel.hextext.client.render.FontRenderContext;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiTextField.class)
public abstract class GuiTextFieldMixin {

    @Unique
    private boolean hextext$legacy$rawPushed;

    @Inject(method = "drawTextBox", at = @At("HEAD"))
    private void hextext$legacy$beginRawMode(CallbackInfo ci) {
        FontRenderContext.pushRawTextRendering();
        hextext$legacy$rawPushed = true;
    }

    @Inject(method = "drawTextBox", at = @At("RETURN"))
    private void hextext$legacy$endRawMode(CallbackInfo ci) {
        if (hextext$legacy$rawPushed) {
            FontRenderContext.popRawTextRendering();
            hextext$legacy$rawPushed = false;
        }
    }
}
