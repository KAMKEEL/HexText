package kamkeel.hextext.mixin.early.impl;

import kamkeel.hextext.common.sign.SignSide;
import kamkeel.hextext.common.sign.SignSideHelper;
import kamkeel.hextext.common.sign.SignState;
import net.minecraft.block.BlockSign;
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
public abstract class MixinBlockSign {

    @Inject(method = "onBlockActivated", at = @At("HEAD"), cancellable = true)
    private void hextext$onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side,
            float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
        if (player == null || player.isSneaking()) {
            return;
        }

        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (!(tileEntity instanceof TileEntitySign)) {
            return;
        }

        TileEntitySign sign = (TileEntitySign) tileEntity;
        SignState signState = (SignState) sign;

        if (signState.hextext$isWaxed()) {
            cir.setReturnValue(true);
            return;
        }

        SignSide clickedSide = SignSideHelper.determineSide(sign, player.posX, player.posZ);
        ItemStack stack = player.getCurrentEquippedItem();

        if (stack != null) {
            if (world.isRemote) {
                cir.setReturnValue(true);
                return;
            }

            if (stack.getItem() == Items.glowstone_dust) {
                if (signState.hextext$setGlowing(clickedSide, true)) {
                    hextext$consumeItem(player, stack);
                    world.markBlockForUpdate(x, y, z);
                }
                cir.setReturnValue(true);
                return;
            }

            if (stack.getItem() == Items.dye && stack.getItemDamage() == 0) {
                if (signState.hextext$setGlowing(clickedSide, false)) {
                    hextext$consumeItem(player, stack);
                    world.markBlockForUpdate(x, y, z);
                }
                cir.setReturnValue(true);
                return;
            }

            if (stack.getItem() == Items.slime_ball) {
                if (!signState.hextext$isWaxed()) {
                    signState.hextext$setWaxed(true);
                    hextext$consumeItem(player, stack);
                    world.markBlockForUpdate(x, y, z);
                }
                cir.setReturnValue(true);
            }
            return;
        }

        signState.hextext$prepareForEdit(clickedSide);
        sign.func_145912_a(player);
        if (!world.isRemote) {
            player.func_146100_a(sign);
        }
        cir.setReturnValue(true);
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
