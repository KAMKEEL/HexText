package kamkeel.hextext.api.rendering;

/**
 * Provides helpers for computing highlight information when rendering raw text in HexText-aware interfaces.
 */
public interface TokenHighlightService {

    /**
     * Abstraction used to query the rendered width of text segments. This is
     * typically backed by the active {@code FontRenderer} instance when used on
     * the client.
     */
    interface WidthProvider {
        int getStringWidth(String text);
    }

    /**
     * Measures the rendered width of the specified literal substring using the
     * supplied {@link WidthProvider}.
     */
    float measureLiteralWidth(WidthProvider provider, CharSequence text, int start, int length);

    /**
     * Returns the highlight colour HexText would use for the token starting at
     * {@code index}.
     */
    int getTokenHighlightColor(CharSequence text, int index);

    /**
     * Creates a highlight descriptor using HexText's standard data carrier.
     */
    HighlightSpan createHighlight(float x, float y, float width, int color);
}
