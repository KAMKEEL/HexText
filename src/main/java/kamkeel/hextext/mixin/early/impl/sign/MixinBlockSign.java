package kamkeel.hextext.mixin.early.impl.sign;

import kamkeel.hextext.HexText;
import kamkeel.hextext.api.sign.HexTextSignInteractions;
import kamkeel.hextext.api.sign.SignInteractionType;
import kamkeel.hextext.common.sign.IHexTextSign;
import kamkeel.hextext.common.sign.SignSide;
import kamkeel.hextext.common.sign.SignSideHelper;
import kamkeel.hextext.common.util.ItemHelper;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockSign;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Set;


@Mixin(BlockSign.class)
public abstract class MixinBlockSign extends BlockContainer {


    protected MixinBlockSign(Material p_i45386_1_) {
        super(p_i45386_1_);
    }

    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer player, int side, float subX, float subY, float subZ) {
        if (player == null || player.isSneaking()) {
            return false;
        }

        TileEntity tileEntity = worldIn.getTileEntity(x, y, z);
        if (!(tileEntity instanceof TileEntitySign)) {
            return false;
        }

        TileEntitySign sign = (TileEntitySign) tileEntity;
        IHexTextSign hexTextSign = (IHexTextSign) sign;

        if (hexTextSign.isWaxed()) {
            return true;
        }

        SignSide clickedSide = SignSideHelper.determineSide(sign, player.posX, player.posZ, subX, subZ);
        ItemStack stack = player.getCurrentEquippedItem();

        if (stack != null) {
            Set<SignInteractionType> interactions = HexTextSignInteractions.getInteractions(stack);
            if (!interactions.isEmpty()) {
                if (worldIn.isRemote) {
                    return true;
                }

                boolean consumed = false;
                boolean updated = false;

                if (interactions.contains(SignInteractionType.CLEANSE)) {
                    boolean changed = false;
                    changed |= hexTextSign.setGlowing(clickedSide, false);
                    changed |= hexTextSign.setOutlined(clickedSide, false);
                    if (changed) {
                        updated = true;
                        if (!consumed) {
                            ItemHelper.consumeItem(player, stack);
                            consumed = true;
                        }
                    }
                }

                if (interactions.contains(SignInteractionType.WAX) && !hexTextSign.isWaxed()) {
                    hexTextSign.setWaxed(true);
                    updated = true;
                    if (!consumed) {
                        ItemHelper.consumeItem(player, stack);
                        consumed = true;
                    }
                }

                if (interactions.contains(SignInteractionType.GLOW)) {
                    boolean changed = hexTextSign.setGlowing(clickedSide, true);
                    if (changed) {
                        updated = true;
                        if (!consumed) {
                            ItemHelper.consumeItem(player, stack);
                            consumed = true;
                        }
                    }
                }

                if (interactions.contains(SignInteractionType.OUTLINE)) {
                    boolean changed = hexTextSign.setOutlined(clickedSide, true);
                    if (changed) {
                        updated = true;
                        if (!consumed) {
                            ItemHelper.consumeItem(player, stack);
                            consumed = true;
                        }
                    }
                }

                if (updated) {
                    worldIn.markBlockForUpdate(x, y, z);
                    tileEntity.markDirty();
                }
                return true;
            }
        }

        if (stack != null) {
            return false;
        }

        if (!HexText.getActiveProxy().allowSignEditing()) {
            return false;
        }

        sign.func_145912_a(player);
        if (worldIn.isRemote) {
            hexTextSign.setEditSide(clickedSide);
            player.func_146100_a(sign);
        }

        return true;
    }
}
