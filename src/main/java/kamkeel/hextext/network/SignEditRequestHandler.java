package kamkeel.hextext.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import kamkeel.hextext.HexText;
import kamkeel.hextext.api.sign.IHexTextSign;
import kamkeel.hextext.common.sign.SignBanHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.WorldServer;

/**
 * Handles incoming sign edit requests from clients on the server side.
 */
public class SignEditRequestHandler implements IMessageHandler<SignEditRequestMessage, IMessage> {

    @Override
    public IMessage onMessage(SignEditRequestMessage message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        WorldServer world = player.getServerForPlayer();

        if (!HexText.getActiveProxy().allowSignEditing()) {
            return null;
        }

        if (!world.blockExists(message.x, message.y, message.z)) {
            return null;
        }

        double dx = player.posX - (message.x + 0.5);
        double dy = player.posY - (message.y + 0.5);
        double dz = player.posZ - (message.z + 0.5);
        if (dx * dx + dy * dy + dz * dz > 64.0) {
            return null;
        }

        TileEntity tileEntity = world.getTileEntity(message.x, message.y, message.z);
        if (!(tileEntity instanceof TileEntitySign)) {
            return null;
        }

        TileEntitySign sign = (TileEntitySign) tileEntity;
        IHexTextSign hexTextSign = (IHexTextSign) sign;

        if (hexTextSign.isWaxed()) {
            return null;
        }

        if (SignBanHelper.isSignBanned(sign)) {
            return null;
        }

        sign.func_145912_a(player);
        return null;
    }
}
