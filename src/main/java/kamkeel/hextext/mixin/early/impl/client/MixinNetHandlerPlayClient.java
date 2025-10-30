package kamkeel.hextext.mixin.early.impl.client;

import kamkeel.hextext.common.sign.SignSide;
import kamkeel.hextext.common.sign.SignState;
import kamkeel.hextext.common.sign.SignSyncPacket;
import kamkeel.hextext.common.util.SignTextHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S33PacketUpdateSign;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    @Shadow
    private Minecraft gameController;

    @Inject(method = "handleUpdateSign", at = @At("TAIL"))
    private void hextext$applySyncData(S33PacketUpdateSign packet, CallbackInfo ci) {
        TileEntity tileEntity = gameController.theWorld.getTileEntity(packet.func_149346_c(),
            packet.func_149345_d(), packet.func_149344_e());
        if (!(tileEntity instanceof TileEntitySign)) {
            return;
        }

        TileEntitySign sign = (TileEntitySign) tileEntity;
        SignState state = (SignState) sign;
        SignSyncPacket sync = (SignSyncPacket) packet;

        String[] backLines = sync.hextext$getBackText();
        String[] dest = state.hextext$getLines(SignSide.BACK);
        for (int i = 0; i < dest.length && i < backLines.length; i++) {
            dest[i] = SignTextHelper.clampToVisibleLimit(backLines[i]);
        }

        state.hextext$setGlowing(SignSide.FRONT, sync.hextext$isGlowing(SignSide.FRONT));
        state.hextext$setGlowing(SignSide.BACK, sync.hextext$isGlowing(SignSide.BACK));
        state.hextext$setOutlined(SignSide.FRONT, sync.hextext$isOutlined(SignSide.FRONT));
        state.hextext$setOutlined(SignSide.BACK, sync.hextext$isOutlined(SignSide.BACK));
        state.hextext$setWaxed(sync.hextext$isWaxed());
        if (sign.lineBeingEdited < 0) {
            state.hextext$setEditingSide(SignSide.FRONT);
        }
    }
}
