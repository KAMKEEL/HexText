package kamkeel.hextext.api.sign;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Set;

/**
 * Registry exposed to other mods for configuring HexText sign interactions.
 */
public interface SignInteractionRegistry {

    /**
     * Metadata value that matches every sub-type of the registered item.
     */
    int ANY_META = OreDictionary.WILDCARD_VALUE;

    /**
     * Registers the default HexText sign interactions.
     */
    void registerDefaults();

    /**
     * Registers the supplied stack for the given interactions.
     */
    void register(ItemStack stack, SignInteractionType... interactions);

    /**
     * Registers the supplied item and metadata combination for the given interactions.
     */
    void register(Item item, int metadata, Set<SignInteractionType> interactions);

    /**
     * Removes the supplied interactions from the given item and metadata combination.
     */
    void unregister(Item item, int metadata, SignInteractionType... interactions);

    /**
     * Checks whether the stack grants the specified interaction.
     */
    boolean provides(ItemStack stack, SignInteractionType type);

    /**
     * Returns all interactions granted by the supplied stack.
     */
    Set<SignInteractionType> getInteractions(ItemStack stack);
}
