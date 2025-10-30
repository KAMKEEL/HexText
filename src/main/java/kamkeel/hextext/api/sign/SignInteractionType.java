package kamkeel.hextext.api.sign;

/**
 * The different interactions that can be performed on a HexText sign
 * using an item. An item may provide multiple interactions at once.
 */
public enum SignInteractionType {
    /** Applies wax to the sign, preventing further edits. */
    WAX,
    /** Enables the glowing text effect for the interacted side. */
    GLOW,
    /** Enables the text outline effect for the interacted side. */
    OUTLINE,
    /** Removes side-specific effects such as glow and outline. */
    CLEANSE
}
