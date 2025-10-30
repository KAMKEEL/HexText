package kamkeel.hextext.api.sign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Public API for configuring the items that interact with HexText signs.
 * <p>
 * Items can grant one or more {@link SignInteractionType} behaviours.
 * Mods can call the registration methods during or after {@code FMLPreInitializationEvent}.
 */
public final class HexTextSignInteractions {

    /** Metadata value that matches every sub-type of the registered item. */
    public static final int ANY_META = OreDictionary.WILDCARD_VALUE;

    private static final Map<Item, List<InteractionEntry>> REGISTRY = new IdentityHashMap<>();
    private static volatile boolean defaultsRegistered = false;

    private HexTextSignInteractions() {
    }

    /**
     * Registers the default vanilla-style sign interactions provided by HexText.
     * This method is automatically invoked by the mod, but exposed for completeness.
     */
    public static void registerDefaults() {
        if (defaultsRegistered) {
            return;
        }
        synchronized (REGISTRY) {
            if (defaultsRegistered) {
                return;
            }
            register(Items.glowstone_dust, ANY_META, EnumSet.of(SignInteractionType.GLOW));
            register(Items.redstone, ANY_META, EnumSet.of(SignInteractionType.OUTLINE));
            register(Items.slime_ball, ANY_META, EnumSet.of(SignInteractionType.WAX));
            register(Items.dye, 0, EnumSet.of(SignInteractionType.CLEANSE));
            defaultsRegistered = true;
        }
    }

    /**
     * Registers the supplied item stack for the provided interactions.
     *
     * @param stack        the representative item stack
     * @param interactions the interactions to register
     */
    public static void register(ItemStack stack, SignInteractionType... interactions) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        register(stack.getItem(), stack.getItemDamage(), toEnumSet(interactions));
    }

    /**
     * Registers the supplied item for the provided interactions.
     *
     * @param item         the item to register
     * @param metadata     the metadata or {@link #ANY_META}
     * @param interactions the interactions to register
     */
    public static void register(Item item, int metadata, Set<SignInteractionType> interactions) {
        if (item == null || interactions == null || interactions.isEmpty()) {
            return;
        }
        EnumSet<SignInteractionType> copy = EnumSet.copyOf(interactions);
        synchronized (REGISTRY) {
            List<InteractionEntry> entries = REGISTRY.computeIfAbsent(item, ignored -> new ArrayList<>());
            InteractionEntry entry = findEntry(entries, metadata);
            if (entry == null) {
                entry = new InteractionEntry(metadata, copy);
                entries.add(entry);
            } else {
                entry.interactions.addAll(copy);
            }
        }
    }

    /**
     * Removes the given interactions for the supplied item metadata combination.
     *
     * @param item         the item to modify
     * @param metadata     the metadata or {@link #ANY_META}
     * @param interactions the interactions to remove
     */
    public static void unregister(Item item, int metadata, SignInteractionType... interactions) {
        if (item == null || interactions == null || interactions.length == 0) {
            return;
        }
        EnumSet<SignInteractionType> removal = toEnumSet(interactions);
        synchronized (REGISTRY) {
            List<InteractionEntry> entries = REGISTRY.get(item);
            if (entries == null) {
                return;
            }
            InteractionEntry entry = findEntry(entries, metadata);
            if (entry == null) {
                return;
            }
            entry.interactions.removeAll(removal);
            if (entry.interactions.isEmpty()) {
                entries.remove(entry);
                if (entries.isEmpty()) {
                    REGISTRY.remove(item);
                }
            }
        }
    }

    /**
     * Determines whether the supplied stack grants the requested interaction.
     *
     * @param stack the item stack to inspect
     * @param type  the interaction type
     * @return {@code true} when the stack grants the interaction
     */
    public static boolean provides(ItemStack stack, SignInteractionType type) {
        if (stack == null || type == null) {
            return false;
        }
        return getInteractions(stack).contains(type);
    }

    /**
     * Returns the interactions provided by the supplied stack.
     *
     * @param stack the stack to inspect
     * @return an immutable set of interactions
     */
    public static Set<SignInteractionType> getInteractions(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return Collections.emptySet();
        }
        EnumSet<SignInteractionType> result = EnumSet.noneOf(SignInteractionType.class);
        Item item = stack.getItem();
        int metadata = stack.getItemDamage();
        synchronized (REGISTRY) {
            List<InteractionEntry> entries = REGISTRY.get(item);
            if (entries == null) {
                return Collections.emptySet();
            }
            for (InteractionEntry entry : entries) {
                if (entry.matches(metadata)) {
                    result.addAll(entry.interactions);
                }
            }
        }
        return result.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(result);
    }

    private static InteractionEntry findEntry(List<InteractionEntry> entries, int metadata) {
        for (InteractionEntry entry : entries) {
            if (entry.metadata == metadata) {
                return entry;
            }
        }
        return null;
    }

    private static EnumSet<SignInteractionType> toEnumSet(SignInteractionType... interactions) {
        EnumSet<SignInteractionType> set = EnumSet.noneOf(SignInteractionType.class);
        if (interactions != null) {
            Collections.addAll(set, interactions);
        }
        return set;
    }

    private static final class InteractionEntry {
        private final int metadata;
        private final EnumSet<SignInteractionType> interactions;

        private InteractionEntry(int metadata, EnumSet<SignInteractionType> interactions) {
            this.metadata = metadata;
            this.interactions = interactions;
        }

        private boolean matches(int stackMeta) {
            return metadata == ANY_META || metadata == stackMeta;
        }
    }
}
