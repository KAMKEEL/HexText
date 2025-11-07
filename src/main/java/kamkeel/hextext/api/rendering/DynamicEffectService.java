package kamkeel.hextext.api.rendering;

/**
 * Provides deterministic helpers for reproducing HexText's dynamic text effects outside of the
 * built-in font renderer.
 */
public interface DynamicEffectService {

    /**
     * Computes the RGB colour that should be displayed for a glyph affected by the rainbow effect.
     *
     * @param now         current time in milliseconds
     * @param glyphIndex  zero-based index of the glyph being rendered
     * @param anchorIndex zero-based index of the glyph that anchors the rainbow effect
     * @return 24-bit RGB colour without an alpha component
     */
    int computeRainbowColor(long now, int glyphIndex, int anchorIndex);

    /**
     * Computes the RGB colour for a glyph affected by the ignite effect given its base colour.
     *
     * @param now       current time in milliseconds
     * @param baseColor 24-bit RGB colour used as the ignite base
     * @return 24-bit RGB colour without an alpha component
     */
    int computeIgniteColor(long now, int baseColor);

    /**
     * Computes the vertical shake offset that should be applied to the current glyph.
     *
     * @param now        current time in milliseconds
     * @param glyphIndex zero-based index of the glyph being rendered
     * @return vertical offset in pixels that should be applied to the glyph
     */
    float computeShakeOffset(long now, int glyphIndex);
}
