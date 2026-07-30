package kamkeel.hextext.common.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayerMP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Bridges HexText's server-side sign handling back onto Bukkit's {@code SignChangeEvent}.
 *
 * <p>HexText replaces {@code NetHandlerPlayServer#processUpdateSign} outright so it can keep colour
 * codes that vanilla would reject. On a Bukkit-flavoured server (Crucible, Thermos, Cauldron) that
 * method is also the only place {@code SignChangeEvent} is fired, so cancelling it silently strips
 * every plugin out of the sign pipeline - Essentials never sees {@code [Buy]}, protection plugins
 * never see the edit, and colour-permission filters never run. Firing the event here restores them.
 *
 * <p>Everything is reflective: HexText has to keep loading on a plain Forge server where none of
 * these classes exist. When Bukkit is absent every entry point degrades to "no plugins objected".
 */
public final class BukkitSignCompatibility {

    private static final Logger LOGGER = LogManager.getLogger("HexText|BukkitCompat");

    private static final String BUKKIT_CLASS = "org.bukkit.Bukkit";
    private static final String PLUGIN_MANAGER_CLASS = "org.bukkit.plugin.PluginManager";
    private static final String EVENT_CLASS = "org.bukkit.event.Event";
    private static final String CANCELLABLE_CLASS = "org.bukkit.event.Cancellable";
    private static final String SIGN_CHANGE_EVENT_CLASS = "org.bukkit.event.block.SignChangeEvent";
    private static final String BLOCK_CLASS = "org.bukkit.block.Block";
    private static final String PLAYER_CLASS = "org.bukkit.entity.Player";

    private static volatile Boolean available;

    private static Method bukkitGetPluginManager;
    private static Method pluginManagerCallEvent;
    private static Method entityGetBukkitEntity;
    private static Method bukkitEntityGetWorld;
    private static Method worldGetBlockAt;
    private static Method eventIsCancelled;
    private static Method eventGetLine;
    private static Constructor<?> signChangeEventConstructor;

    private BukkitSignCompatibility() {}

    /**
     * Returns {@code true} when a Bukkit server implementation is present and every reflective
     * handle needed to fire {@code SignChangeEvent} resolved.
     */
    public static boolean isAvailable() {
        Boolean cached = available;
        if (cached != null) {
            return cached;
        }
        synchronized (BukkitSignCompatibility.class) {
            if (available == null) {
                available = resolve();
            }
            return available;
        }
    }

    private static boolean resolve() {
        try {
            ClassLoader loader = BukkitSignCompatibility.class.getClassLoader();

            Class<?> bukkit = Class.forName(BUKKIT_CLASS, false, loader);
            Class<?> pluginManager = Class.forName(PLUGIN_MANAGER_CLASS, false, loader);
            Class<?> event = Class.forName(EVENT_CLASS, false, loader);
            Class<?> cancellable = Class.forName(CANCELLABLE_CLASS, false, loader);
            Class<?> signChangeEvent = Class.forName(SIGN_CHANGE_EVENT_CLASS, false, loader);
            Class<?> block = Class.forName(BLOCK_CLASS, false, loader);
            Class<?> player = Class.forName(PLAYER_CLASS, false, loader);
            Class<?> bukkitEntity = Class.forName("org.bukkit.entity.Entity", false, loader);
            Class<?> bukkitWorld = Class.forName("org.bukkit.World", false, loader);

            bukkitGetPluginManager = bukkit.getMethod("getPluginManager");
            pluginManagerCallEvent = pluginManager.getMethod("callEvent", event);
            // Crucible puts getBukkitEntity() on net.minecraft.entity.Entity, so every player has it.
            entityGetBukkitEntity = net.minecraft.entity.Entity.class.getMethod("getBukkitEntity");
            bukkitEntityGetWorld = bukkitEntity.getMethod("getWorld");
            worldGetBlockAt = bukkitWorld.getMethod("getBlockAt", int.class, int.class, int.class);
            eventIsCancelled = cancellable.getMethod("isCancelled");
            eventGetLine = signChangeEvent.getMethod("getLine", int.class);
            signChangeEventConstructor = signChangeEvent.getConstructor(block, player, String[].class);

            LOGGER.info("Bukkit server detected; HexText will fire SignChangeEvent for sign edits.");
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            // The overwhelmingly common case: a plain Forge server with no Bukkit at all.
            LOGGER.debug("Bukkit sign API unavailable; plugin sign hooks disabled", ex);
            return false;
        } catch (LinkageError | RuntimeException ex) {
            LOGGER.warn("Bukkit sign API present but unusable; plugin sign hooks disabled", ex);
            return false;
        }
    }

    /**
     * Fires {@code SignChangeEvent} for the given edit and reports what the plugins decided.
     *
     * <p>Fails open: if Bukkit is missing, or anything about the reflective call goes wrong, the
     * result carries the original lines uncancelled so sign editing keeps working regardless.
     *
     * @param player the editing player
     * @param x      sign block X
     * @param y      sign block Y
     * @param z      sign block Z
     * @param lines  the four lines the player submitted; never mutated
     * @return the plugins' verdict, never {@code null}
     */
    public static Result fireSignChange(EntityPlayerMP player, int x, int y, int z, String[] lines) {
        String[] submitted = copyOfFour(lines);
        if (player == null || !isAvailable()) {
            return new Result(false, submitted);
        }

        try {
            Object bukkitPlayer = entityGetBukkitEntity.invoke(player);
            if (bukkitPlayer == null) {
                return new Result(false, submitted);
            }

            Object bukkitWorld = bukkitEntityGetWorld.invoke(bukkitPlayer);
            if (bukkitWorld == null) {
                return new Result(false, submitted);
            }

            Object block = worldGetBlockAt.invoke(bukkitWorld, x, y, z);
            if (block == null) {
                return new Result(false, submitted);
            }

            // The event holds onto this array and setLine() writes straight into it, so hand over a
            // private copy rather than the packet's own storage.
            String[] mutable = copyOfFour(submitted);
            Object event = signChangeEventConstructor.newInstance(block, bukkitPlayer, mutable);

            pluginManagerCallEvent.invoke(bukkitGetPluginManager.invoke(null), event);

            if (Boolean.TRUE.equals(eventIsCancelled.invoke(event))) {
                return new Result(true, submitted);
            }

            String[] edited = new String[4];
            for (int i = 0; i < 4; i++) {
                Object line = eventGetLine.invoke(event, i);
                edited[i] = line == null ? "" : line.toString();
            }
            return new Result(false, edited);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            LOGGER.warn("Failed to fire SignChangeEvent at {}, {}, {}; applying the edit unfiltered", x, y, z, ex);
            return new Result(false, submitted);
        }
    }

    private static String[] copyOfFour(String[] source) {
        String[] copy = new String[4];
        for (int i = 0; i < 4; i++) {
            String value = (source != null && i < source.length) ? source[i] : "";
            copy[i] = value == null ? "" : value;
        }
        return copy;
    }

    /**
     * Outcome of a {@code SignChangeEvent}: whether a plugin vetoed the edit, and the lines as they
     * stood after every plugin had its say.
     */
    public static final class Result {

        private final boolean cancelled;
        private final String[] lines;

        Result(boolean cancelled, String[] lines) {
            this.cancelled = cancelled;
            this.lines = lines;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public String[] getLines() {
            return lines;
        }
    }
}
