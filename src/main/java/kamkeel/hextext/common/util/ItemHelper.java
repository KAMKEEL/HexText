package kamkeel.hextext.common.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ItemHelper {

    public static void consumeItem(EntityPlayer player, ItemStack stack) {
        if (player.capabilities.isCreativeMode) {
            return;
        }
        stack.stackSize--;
        if (stack.stackSize <= 0) {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
        }
    }
}
