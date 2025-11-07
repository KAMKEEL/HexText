package kamkeel.hextext.mixin.early.impl.client.sign;

import kamkeel.hextext.api.sign.IHexTextSign;
import kamkeel.hextext.api.sign.SignSide;
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
        IHexTextSign state = (IHexTextSign) sign;
        SignSyncPacket sync = (SignSyncPacket) packet;

        // copy back lines safely
        SignTextHelper.copyTextClamped(sync.getBackText(), state.getLines(SignSide.BACK));

        state.setGlowing(SignSide.FRONT, sync.isGlowing(SignSide.FRONT));
        state.setGlowing(SignSide.BACK, sync.isGlowing(SignSide.BACK));
        state.setOutlined(SignSide.FRONT, sync.isOutlined(SignSide.FRONT));
        state.setOutlined(SignSide.BACK, sync.isOutlined(SignSide.BACK));
        state.setWaxed(sync.isWaxed());
    }
}
