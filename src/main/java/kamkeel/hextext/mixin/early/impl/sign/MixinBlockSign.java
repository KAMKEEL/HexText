package kamkeel.hextext.mixin.early.impl.sign;

import kamkeel.hextext.HexText;
import kamkeel.hextext.api.HexTextApi;
import kamkeel.hextext.api.sign.IHexTextSign;
import kamkeel.hextext.api.sign.SignInteractionType;
import kamkeel.hextext.api.sign.SignSide;
import kamkeel.hextext.common.sign.SignSideHelper;
import kamkeel.hextext.common.util.ItemHelper;
import kamkeel.hextext.config.HexTextConfig;
import kamkeel.hextext.permissions.HexTextPermissions;
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

    /**
     * Checks if a player can use a specific sign modifier.
     * Uses config as default, with Bukkit permissions as override.
     *
     * @param player The player to check
     * @param type The sign interaction type
     * @return true if allowed, false otherwise
     */
    private boolean canUseSignModifier(EntityPlayer player, SignInteractionType type) {
        boolean configDefault;
        HexTextPermissions.Permission permission;

        switch (type) {
            case GLOW:
                configDefault = HexTextConfig.isGlowstoneDustGlowEnabled();
                permission = HexTextPermissions.SIGN_MODIFIER_GLOW;
                break;
            case OUTLINE:
                configDefault = HexTextConfig.isRedstoneDustOutlineEnabled();
                permission = HexTextPermissions.SIGN_MODIFIER_OUTLINE;
                break;
            case WAX:
                configDefault = HexTextConfig.isSlimeballWaxEnabled();
                permission = HexTextPermissions.SIGN_MODIFIER_WAX;
                break;
            case CLEANSE:
                configDefault = HexTextConfig.isInkSacCleanseEnabled();
                permission = HexTextPermissions.SIGN_MODIFIER_CLEANSE;
                break;
            default:
                return true;
        }

        // If Bukkit permissions are enabled, use permission check
        if (HexTextPermissions.enabled()) {
            return HexTextPermissions.hasPermission(player, permission);
        }

        // Otherwise use config default
        return configDefault;
    }

    /**
     * Checks if a player can edit signs.
     * Uses config as default, with Bukkit permissions as override.
     *
     * @param player The player to check
     * @return true if allowed, false otherwise
     */
    private boolean canEditSign(EntityPlayer player) {
        // If Bukkit permissions are enabled, use permission check
        if (HexTextPermissions.enabled()) {
            return HexTextPermissions.hasPermission(player, HexTextPermissions.SIGN_EDIT);
        }

        // Otherwise use config default
        return HexTextConfig.isSignEditingAllowed();
    }

    public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer player, int side, float subX, float subY, float subZ) {
        if (player == null || player.isSneaking()) {
            return false;
        }

        if (!HexText.getActiveProxy().isRemoteHexTextPresent()) {
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
            Set<SignInteractionType> interactions = HexTextApi.signInteractions().getInteractions(stack);
            if (!interactions.isEmpty()) {
                if (worldIn.isRemote) {
                    return true;
                }

                boolean consumed = false;
                boolean updated = false;

                if (interactions.contains(SignInteractionType.CLEANSE) && canUseSignModifier(player, SignInteractionType.CLEANSE)) {
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

                if (interactions.contains(SignInteractionType.WAX) && !hexTextSign.isWaxed() && canUseSignModifier(player, SignInteractionType.WAX)) {
                    hexTextSign.setWaxed(true);
                    updated = true;
                    if (!consumed) {
                        ItemHelper.consumeItem(player, stack);
                        consumed = true;
                    }
                }

                if (interactions.contains(SignInteractionType.GLOW) && canUseSignModifier(player, SignInteractionType.GLOW)) {
                    boolean changed = hexTextSign.setGlowing(clickedSide, true);
                    if (changed) {
                        updated = true;
                        if (!consumed) {
                            ItemHelper.consumeItem(player, stack);
                            consumed = true;
                        }
                    }
                }

                if (interactions.contains(SignInteractionType.OUTLINE) && canUseSignModifier(player, SignInteractionType.OUTLINE)) {
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

        if (!canEditSign(player)) {
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
