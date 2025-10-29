package kamkeel.hextext.mixin.early.impl;

import kamkeel.hextext.common.sign.SignSide;
import kamkeel.hextext.common.sign.SignState;
import kamkeel.hextext.common.sign.SignUpdatePacket;
import kamkeel.hextext.common.util.SignTextHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C12PacketUpdateSign;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

    @Shadow public EntityPlayerMP playerEntity;

    @Inject(method = "processUpdateSign", at = @At("HEAD"), cancellable = true)
    private void hextext$processUpdateSign(C12PacketUpdateSign packet, CallbackInfo ci) {
        WorldServer world = playerEntity.getServerForPlayer();
        int x = packet.func_149588_c();
        int y = packet.func_149586_d();
        int z = packet.func_149585_e();

        if (!world.blockExists(x, y, z)) {
            ci.cancel();
            return;
        }

        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (!(tileEntity instanceof TileEntitySign)) {
            return;
        }

        TileEntitySign sign = (TileEntitySign) tileEntity;
        if (!sign.func_145914_a() || sign.func_145911_b() != playerEntity) {
            playerEntity.playerNetServerHandler.kickPlayerFromServer("You are not permitted to use this sign!");
            ci.cancel();
            return;
        }

        String[] lines = packet.func_149589_f();
        for (int i = 0; i < lines.length; ++i) {
            String sanitized = ChatAllowedCharacters.filerAllowedCharacters(lines[i]);
            sign.signText[i] = SignTextHelper.clampToVisibleLimit(sanitized);
        }

        SignState state = (SignState) sign;
        SignSide packetSide = ((SignUpdatePacket) packet).hextext$getEditingSide();
        state.hextext$setEditingSide(packetSide);
        state.hextext$finishEdit();
        sign.func_145912_a(null);
        world.markBlockForUpdate(x, y, z);
        ci.cancel();
    }
}
