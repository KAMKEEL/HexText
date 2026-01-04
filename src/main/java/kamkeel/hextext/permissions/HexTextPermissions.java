package kamkeel.hextext.permissions;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import kamkeel.hextext.HexText;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles permission checks for HexText features.
 * Integrates with Bukkit permission systems when available (e.g., MCPC+, Cauldron, Thermos).
 * Falls back to allowing all operations when Bukkit is not present, using config defaults.
 */
public class HexTextPermissions {

    private static final Logger LOGGER = LogManager.getLogger(HexText.ID);

    // Sign Editing Permissions
    public static final Permission SIGN_EDIT = new Permission("hextext.sign.edit");

    // Sign Modifier Permissions
    public static final Permission SIGN_MODIFIER_GLOW = new Permission("hextext.sign.modifier.glow");
    public static final Permission SIGN_MODIFIER_OUTLINE = new Permission("hextext.sign.modifier.outline");
    public static final Permission SIGN_MODIFIER_WAX = new Permission("hextext.sign.modifier.wax");
    public static final Permission SIGN_MODIFIER_CLEANSE = new Permission("hextext.sign.modifier.cleanse");

    // Text Formatting Permissions
    public static final Permission FORMAT_AMPERSAND_CHAT = new Permission("hextext.format.ampersand.chat");
    public static final Permission FORMAT_AMPERSAND_SIGN = new Permission("hextext.format.ampersand.sign");
    public static final Permission FORMAT_AMPERSAND_REPAIR = new Permission("hextext.format.ampersand.repair");
    public static final Permission FORMAT_HTML = new Permission("hextext.format.html");

    public static HexTextPermissions Instance;
    private Class<?> bukkit;
    private Method getPlayer;
    private Method hasPermission;

    public HexTextPermissions() {
        Instance = this;
        try {
            bukkit = Class.forName("org.bukkit.Bukkit");
            getPlayer = bukkit.getMethod("getPlayer", String.class);
            hasPermission = Class.forName("org.bukkit.entity.Player").getMethod("hasPermission", String.class);
            LOGGER.info("Bukkit permissions enabled");
            LOGGER.info("HexText permissions available:");
            Collections.sort(Permission.permissions, String.CASE_INSENSITIVE_ORDER);
            for (String p : Permission.permissions) {
                LOGGER.info("  " + p);
            }
        } catch (ClassNotFoundException e) {
            // Bukkit/MCPC+/Cauldron/Thermos is not loaded - gracefully degrade
            LOGGER.info("Bukkit not detected, using config defaults for all players");
        } catch (NoSuchMethodException e) {
            LOGGER.error("Failed to find Bukkit permission methods", e);
        } catch (SecurityException e) {
            LOGGER.error("Security exception accessing Bukkit methods", e);
        }
    }

    /**
     * Check if a player has the specified permission.
     *
     * Permission check order:
     * 1. Server-side OP status - OPs always have all permissions
     * 2. Bukkit permission system - If available, delegates to Bukkit
     * 3. Default - If no Bukkit, returns true (uses config defaults)
     *
     * @param player The player to check
     * @param permission The permission to check
     * @return true if the player has permission, false otherwise
     */
    public static boolean hasPermission(EntityPlayer player, Permission permission) {
        if (player == null) {
            return true;
        }

        // OPs always have all permissions (server-side only)
        if (FMLCommonHandler.instance().getSide() == Side.SERVER && isOp(player)) {
            return true;
        }

        // Check Bukkit permissions if available
        if (permission != null && Instance != null && Instance.bukkit != null) {
            return Instance.bukkitPermission(player.getCommandSenderName(), permission.name);
        }

        // Default: allow (falls back to config defaults)
        return true;
    }

    /**
     * Check if a player has a custom permission string.
     * Used for dynamic permission checks not covered by static Permission objects.
     *
     * @param player The player to check
     * @param permission The permission string to check
     * @return true if the player has permission, false otherwise
     */
    public static boolean hasPermissionString(EntityPlayer player, String permission) {
        if (player == null) {
            return true;
        }

        // OPs always have all permissions (server-side only)
        if (FMLCommonHandler.instance().getSide() == Side.SERVER && isOp(player)) {
            return true;
        }

        // Check Bukkit permissions if available
        if (Instance != null && Instance.bukkit != null) {
            return Instance.bukkitPermission(player.getCommandSenderName(), permission);
        }

        // Default: allow
        return true;
    }

    /**
     * Check if the Bukkit permission system is enabled.
     *
     * @return true if Bukkit permissions are active
     */
    public static boolean enabled() {
        return Instance != null && Instance.bukkit != null;
    }

    /**
     * Check if a player is a server operator.
     *
     * @param player The player to check
     * @return true if the player is an OP
     */
    public static boolean isOp(EntityPlayer player) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            return false;
        }
        return server.getConfigurationManager().func_152596_g(player.getGameProfile());
    }

    private boolean bukkitPermission(String username, String permission) {
        try {
            Object player = getPlayer.invoke(null, username);
            if (player == null) {
                return true; // Player not found in Bukkit, allow by default
            }
            return (Boolean) hasPermission.invoke(player, permission);
        } catch (IllegalAccessException e) {
            LOGGER.error("IllegalAccessException checking Bukkit permission", e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("IllegalArgumentException checking Bukkit permission", e);
        } catch (InvocationTargetException e) {
            LOGGER.error("InvocationTargetException checking Bukkit permission", e);
        }
        return true; // Default to allow on error
    }

    /**
     * Represents a HexText permission node.
     * Permission nodes follow the format: hextext.<category>.<feature>
     */
    public static class Permission {
        private static final List<String> permissions = new ArrayList<String>();
        public final String name;

        public Permission(String name) {
            this.name = name;
            if (!permissions.contains(name)) {
                permissions.add(name);
            }
        }

        /**
         * Get a list of all registered permission nodes.
         *
         * @return List of permission node names
         */
        public static List<String> getAllPermissions() {
            return new ArrayList<String>(permissions);
        }
    }
}
