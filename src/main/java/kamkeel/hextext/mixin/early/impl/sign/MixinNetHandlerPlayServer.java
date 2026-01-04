package kamkeel.hextext.mixin.early.impl.sign;

import kamkeel.hextext.HexText;
import kamkeel.hextext.api.sign.IHexTextSign;
import kamkeel.hextext.api.sign.SignSide;
import kamkeel.hextext.common.sign.SignUpdatePacket;
import kamkeel.hextext.common.util.SignTextHelper;
import kamkeel.hextext.common.util.StringUtils;
import kamkeel.hextext.config.HexTextConfig;
import kamkeel.hextext.permissions.HexTextPermissions;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

    @Shadow
    public EntityPlayerMP playerEntity;

    /**
     * Checks if the player can edit signs.
     * Uses config as default, with Bukkit permissions as override.
     */
    private boolean canEditSign() {
        if (HexTextPermissions.enabled()) {
            return HexTextPermissions.hasPermission(playerEntity, HexTextPermissions.SIGN_EDIT);
        }
        return HexTextConfig.isSignEditingAllowed();
    }

    /**
     * Checks if the player can use ampersand formatting in chat.
     * Uses config as default, with Bukkit permissions as override.
     */
    private boolean canUseAmpersandInChat() {
        if (HexTextPermissions.enabled()) {
            return HexTextPermissions.hasPermission(playerEntity, HexTextPermissions.FORMAT_AMPERSAND_CHAT);
        }
        return HexText.getActiveProxy().convertAmpersandsInChat();
    }

    /**
     * Checks if the player can use ampersand formatting in repairs.
     * Uses config as default, with Bukkit permissions as override.
     */
    private boolean canUseAmpersandInRepair() {
        if (HexTextPermissions.enabled()) {
            return HexTextPermissions.hasPermission(playerEntity, HexTextPermissions.FORMAT_AMPERSAND_REPAIR);
        }
        return HexText.getActiveProxy().convertAmpersandsInRepairs();
    }

    @Inject(method = "processUpdateSign", at = @At("HEAD"), cancellable = true)
    private void hextext$processUpdateSign(C12PacketUpdateSign packet, CallbackInfo ci) {
        if (!HexText.getActiveProxy().isRemoteHexTextPresent()) {
            return;
        }

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

        // Check HexText permission for sign editing
        if (!canEditSign()) {
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
    public boolean hexText$processChatConvert(char character){
        if(canUseAmpersandInChat() && character == StringUtils.SECTION_SIGN)
            return true;
        return ChatAllowedCharacters.isAllowedCharacter(character);
    }

    @ModifyArg(method = "processVanilla250Packet", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/ContainerRepair;updateItemName(Ljava/lang/String;)V"))
    public String hexText$processRepairConvert(String original){
        if(!canUseAmpersandInRepair())
            return original;
        return StringUtils.convertAmpersandsToSectionSigns(original);
    }
}
