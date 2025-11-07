package kamkeel.hextext.mixin.early.impl.client.sign;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.render.FontRenderContext;
import kamkeel.hextext.api.sign.IHexTextSign;
import kamkeel.hextext.api.sign.SignSide;
import kamkeel.hextext.common.sign.SignUpdatePacket;
import kamkeel.hextext.common.util.SignTextHelper;
import kamkeel.hextext.common.util.StringUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C12PacketUpdateSign;
import net.minecraft.tileentity.TileEntitySign;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(GuiEditSign.class)
public abstract class MixinGuiEditSign extends GuiScreen {

    @Shadow
    private TileEntitySign tileSign;

    @Unique
    public SignSide editSide = SignSide.FRONT;

    @Unique
    public String[] editLines;

    @Unique
    private boolean hextext$rawRenderingActive;

    @Inject(method = "initGui", at = @At(value = "HEAD"))
    private void initSide(CallbackInfo ci) {
        IHexTextSign hexTextSign = (IHexTextSign) tileSign;
        this.editSide = hexTextSign.getEditSide();
        this.editLines = hexTextSign.getLines(this.editSide);

        if (HexText.getActiveProxy().convertAmpersandsOnSigns()){
            for (int i = 0; i < this.editLines.length; i++) {
                this.editLines[i] = StringUtils.convertSectionSignsToAmpersands(this.editLines[i]);
            }
        }
    }

    /**
     * Right before the TE render call in drawScreen, add an extra 180° yaw if the GUI is editing the BACK side.
     * Vanilla has a PushMatrix at the start of drawScreen and PopMatrix after the render, so this is self-contained.
     */
    @Inject(
        method = "drawScreen(IIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;renderTileEntityAt(Lnet/minecraft/tileentity/TileEntity;DDDF)V",
            shift = At.Shift.BEFORE
        )
    )
    private void hextext$rotateForBackSide(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        SignSide side = ((IHexTextSign) this.tileSign).getEditSide();
        if (side == SignSide.BACK) {
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
        }
    }

    @Inject(
        method = "drawScreen(IIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;renderTileEntityAt(Lnet/minecraft/tileentity/TileEntity;DDDF)V",
            shift = At.Shift.BEFORE
        )
    )
    private void hextext$enableRawRendering(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (HexText.getActiveProxy() == null) {
            return;
        }

        if (!HexText.getActiveProxy().convertAmpersandsOnSigns()
            && !HexText.getActiveProxy().allowUniversalAmpersand()) {
            return;
        }

        FontRenderContext.pushRawTextRendering();
        HexText.rawRenderingEnabled = true;
        hextext$rawRenderingActive = true;
    }

    @Inject(
        method = "drawScreen(IIF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;renderTileEntityAt(Lnet/minecraft/tileentity/TileEntity;DDDF)V",
            shift = At.Shift.AFTER
        )
    )
    private void hextext$disableRawRendering(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!hextext$rawRenderingActive) {
            return;
        }

        FontRenderContext.popRawTextRendering();
        HexText.rawRenderingEnabled = false;
        hextext$rawRenderingActive = false;
    }

    /**
     * Redirects all reads of this.tileSign.signText in keyTyped
     * to use the HexText per-side buffer instead.
     */
    @Redirect(
        method = "keyTyped",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/tileentity/TileEntitySign;signText:[Ljava/lang/String;"
        )
    )
    private String[] hextext$useCustomSignBuffer(TileEntitySign sign) {
        return this.editLines;
    }

    @Redirect(method = "keyTyped", at = @At(value = "INVOKE", target = "Ljava/lang/String;length()I", ordinal = 2))
    private int hextext$measureVisibleLength(String line) {
        return SignTextHelper.visibleLength(line);
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"), cancellable = true)
    private void hextext$sendExtendedPacket(CallbackInfo ci) {
        ci.cancel();
        Keyboard.enableRepeatEvents(false);
        NetHandlerPlayClient handler = this.mc.getNetHandler();
        if (HexText.getActiveProxy().convertAmpersandsOnSigns()){
            for (int i = 0; i < this.editLines.length; i++) {
                String converted = StringUtils.convertAmpersandsToSectionSigns(this.editLines[i]);
                this.editLines[i] = converted;
            }
        }

        if (handler != null) {
            C12PacketUpdateSign packet = new C12PacketUpdateSign(
                tileSign.xCoord, tileSign.yCoord, tileSign.zCoord, this.editLines
            );
            ((SignUpdatePacket) packet).setSide(editSide);
            handler.addToSendQueue(packet);
        }
        tileSign.setEditable(true);
    }
}
