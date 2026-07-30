package kamkeel.hextext.mixin.early.impl.sign;

import kamkeel.hextext.HexText;
import kamkeel.hextext.api.sign.IHexTextSign;
import kamkeel.hextext.api.sign.SignSide;
import kamkeel.hextext.common.compat.BukkitSignCompatibility;
import kamkeel.hextext.common.sign.SignBanHelper;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

    @Shadow
    public EntityPlayerMP playerEntity;

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
            if (BukkitSignCompatibility.isAvailable()) {
                // CraftBukkit deliberately dropped vanilla's kick here in favour of resyncing the
                // sign, because plugins legitimately reopen sign editors. Match that on Bukkit
                // servers so HexText does not reintroduce a kick the server had removed.
                world.markBlockForUpdate(x, y, z);
            } else {
                playerEntity.playerNetServerHandler.kickPlayerFromServer("You are not permitted to use this sign!");
            }
            ci.cancel();
            return;
        }

        if (SignBanHelper.isSignBanned(sign)) {
            sign.func_145912_a(null);
            ci.cancel();
            return;
        }

        SignSide side = ((SignUpdatePacket) packet).getSide();
        IHexTextSign state = (IHexTextSign) sign;
        String[] dest = state.getLines(side);

        // Sanitize and clamp before the event, not after: a plugin that validates the text must see
        // exactly what will be stored. Handing Essentials an unclamped 16-digit price it approves,
        // then trimming it to 15 behind its back, would leave the sign charging a figure no plugin
        // ever agreed to.
        String[] lines = new String[4];
        SignTextHelper.copyTextSanitizedClamped(packet.func_149589_f(), lines);

        // SignChangeEvent models a single-sided vanilla sign, and plugins that act on it (Essentials'
        // [Buy], protection, colour filters) read line 0 back off the block's front face. Firing it
        // for a back-face edit would make those plugins act on text the block never shows, so the
        // back side stays a HexText-only concern.
        if (side == SignSide.FRONT) {
            BukkitSignCompatibility.Result result =
                BukkitSignCompatibility.fireSignChange(playerEntity, x, y, z, lines);
            if (result.isCancelled()) {
                sign.func_145912_a(null);
                world.markBlockForUpdate(x, y, z);
                ci.cancel();
                return;
            }
            lines = result.getLines();
        }

        // Clamp again: plugins rewrite lines too (Essentials swaps line 0 for its success name).
        SignTextHelper.copyTextSanitizedClamped(lines, dest);

        sign.func_145912_a(null);
        tileEntity.markDirty();
        world.markBlockForUpdate(x, y, z);
        ci.cancel();
    }

    @Redirect(method = "processChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ChatAllowedCharacters;isAllowedCharacter(C)Z"))
    public boolean hexText$processChatConvert(char character){
        if(HexText.getActiveProxy().convertAmpersandsInChat() && character == StringUtils.SECTION_SIGN)
            return true;
        return ChatAllowedCharacters.isAllowedCharacter(character);
    }

    @ModifyArg(method = "processVanilla250Packet", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/ContainerRepair;updateItemName(Ljava/lang/String;)V"))
    public String hexText$processRepairConvert(String original){
        if(!HexText.getActiveProxy().convertAmpersandsInRepairs())
            return original;
        return StringUtils.convertAmpersandsToSectionSigns(original);
    }
}
