package kamkeel.hextext.common.sign;

import kamkeel.hextext.api.sign.SignInteractionRegistry;
import kamkeel.hextext.api.sign.SignInteractionType;
import kamkeel.hextext.config.HexTextConfig;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of the {@link SignInteractionRegistry} backed by HexText's configuration.
 */
public final class HexTextSignInteractions implements SignInteractionRegistry {

    private final Map<Item, List<InteractionEntry>> registry = new IdentityHashMap<>();
    private volatile boolean defaultsRegistered;

    @Override
    public void registerDefaults() {
        if (defaultsRegistered) {
            return;
        }
        synchronized (registry) {
            if (defaultsRegistered) {
                return;
            }
            if (HexTextConfig.isGlowstoneDustGlowEnabled()) {
                register(Items.glowstone_dust, ANY_META, EnumSet.of(SignInteractionType.GLOW));
            }
            if (HexTextConfig.isRedstoneDustOutlineEnabled()) {
                register(Items.redstone, ANY_META, EnumSet.of(SignInteractionType.OUTLINE));
            }
            if (HexTextConfig.isSlimeballWaxEnabled()) {
                register(Items.slime_ball, ANY_META, EnumSet.of(SignInteractionType.WAX));
            }
            if (HexTextConfig.isInkSacCleanseEnabled()) {
                register(Items.dye, 0, EnumSet.of(SignInteractionType.CLEANSE));
            }
            defaultsRegistered = true;
        }
    }

    @Override
    public void register(ItemStack stack, SignInteractionType... interactions) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        register(stack.getItem(), stack.getItemDamage(), toEnumSet(interactions));
    }

    @Override
    public void register(Item item, int metadata, Set<SignInteractionType> interactions) {
        if (item == null || interactions == null || interactions.isEmpty()) {
            return;
        }
        EnumSet<SignInteractionType> copy = EnumSet.copyOf(interactions);
        synchronized (registry) {
            List<InteractionEntry> entries = registry.computeIfAbsent(item, ignored -> new ArrayList<>());
            InteractionEntry entry = findEntry(entries, metadata);
            if (entry == null) {
                entry = new InteractionEntry(metadata, copy);
                entries.add(entry);
            } else {
                entry.interactions.addAll(copy);
            }
        }
    }

    @Override
    public void unregister(Item item, int metadata, SignInteractionType... interactions) {
        if (item == null || interactions == null || interactions.length == 0) {
            return;
        }
        EnumSet<SignInteractionType> removal = toEnumSet(interactions);
        synchronized (registry) {
            List<InteractionEntry> entries = registry.get(item);
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
                    registry.remove(item);
                }
            }
        }
    }

    @Override
    public boolean provides(ItemStack stack, SignInteractionType type) {
        if (stack == null || type == null) {
            return false;
        }
        return getInteractions(stack).contains(type);
    }

    @Override
    public Set<SignInteractionType> getInteractions(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return Collections.emptySet();
        }
        EnumSet<SignInteractionType> result = EnumSet.noneOf(SignInteractionType.class);
        Item item = stack.getItem();
        int metadata = stack.getItemDamage();
        synchronized (registry) {
            List<InteractionEntry> entries = registry.get(item);
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
