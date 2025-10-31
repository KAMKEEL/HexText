package kamkeel.hextext.mixin.early.impl.sign;

import kamkeel.hextext.HexText;
import kamkeel.hextext.common.sign.IHexTextSign;
import kamkeel.hextext.common.sign.SignSide;
import kamkeel.hextext.common.sign.SignUpdatePacket;
import kamkeel.hextext.common.util.SignTextHelper;
import kamkeel.hextext.common.util.StringUtils;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

    @Shadow
    public EntityPlayerMP playerEntity;

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

        SignSide side = ((SignUpdatePacket) packet).getSide();
        IHexTextSign state = (IHexTextSign) sign;
        String[] dest = state.getLines(side);

        SignTextHelper.copyTextSanitizedClamped(packet.func_149589_f(), dest);

        sign.func_145912_a(null);
        tileEntity.markDirty();
        world.markBlockForUpdate(x, y, z);
        ci.cancel();
    }

    @Redirect(method = "processChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ChatAllowedCharacters;isAllowedCharacter(C)Z"))
    public boolean hexText$processChatMessage(char character){
        if(HexText.getActiveProxy().allowUniversalAmpersand() && character == StringUtils.SECTION_SIGN)
            return true;
        return ChatAllowedCharacters.isAllowedCharacter(character);
    }
}
