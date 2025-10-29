package kamkeel.hextext.mixin.early.impl;

import kamkeel.hextext.common.sign.SignSide;
import kamkeel.hextext.common.sign.SignSideHelper;
import kamkeel.hextext.common.sign.SignState;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockSign;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockSign.class)
public abstract class MixinBlockSign extends BlockContainer {


    protected MixinBlockSign(Material p_i45386_1_) {
        super(p_i45386_1_);
    }

    @Unique
    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer player, int side, float subX, float subY, float subZ)
    {
        if (player == null || player.isSneaking()) {
            return false;
        }

        TileEntity tileEntity = worldIn.getTileEntity(x, y, z);
        if (!(tileEntity instanceof TileEntitySign)) {
            return false;
        }

        TileEntitySign sign = (TileEntitySign) tileEntity;
        SignState signState = (SignState) sign;

        if (signState.hextext$isWaxed()) {
            return true;
        }

        SignSide clickedSide = SignSideHelper.determineSide(sign, player.posX, player.posZ);
        ItemStack stack = player.getCurrentEquippedItem();

        if (stack != null) {
            if (worldIn.isRemote) {
                return true;
            }

            if (stack.getItem() == Items.glowstone_dust) {
                if (signState.hextext$setGlowing(clickedSide, true)) {
                    hextext$consumeItem(player, stack);
                    worldIn.markBlockForUpdate(x, y, z);
                }
                return true;
            }

            if (stack.getItem() == Items.dye && stack.getItemDamage() == 0) {
                if (signState.hextext$setGlowing(clickedSide, false)) {
                    hextext$consumeItem(player, stack);
                    worldIn.markBlockForUpdate(x, y, z);
                }
                return true;
            }

            if (stack.getItem() == Items.slime_ball) {
                if (!signState.hextext$isWaxed()) {
                    signState.hextext$setWaxed(true);
                    hextext$consumeItem(player, stack);
                    worldIn.markBlockForUpdate(x, y, z);
                }
                return true;
            }
        }

        signState.hextext$prepareForEdit(clickedSide);
        sign.func_145912_a(player);
        if (!worldIn.isRemote) {
            player.func_146100_a(sign);
        }

        return true;
    }

    @Unique
    private void hextext$consumeItem(EntityPlayer player, ItemStack stack) {
        if (player.capabilities.isCreativeMode) {
            return;
        }
        stack.stackSize--;
        if (stack.stackSize <= 0) {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        }
    }
}
