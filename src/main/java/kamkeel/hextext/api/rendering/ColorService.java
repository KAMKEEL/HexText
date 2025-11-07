package kamkeel.hextext.api.rendering;

/**
 * Helper utilities for colour calculations performed by the HexText renderer.
 */
public interface ColorService {

    /**
     * Computes the default drop-shadow colour derived from the supplied RGB value.
     *
     * @param rgb 24-bit RGB colour without an alpha component
     * @return 24-bit RGB colour representing the recommended shadow shade
     */
    int calculateShadowColor(int rgb);
}
