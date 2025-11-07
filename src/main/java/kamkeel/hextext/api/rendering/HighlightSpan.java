package kamkeel.hextext.api.rendering;

/**
 * Describes a highlighted token region that should be drawn behind rendered text.
 */
public interface HighlightSpan {

    float getX();

    float getY();

    float getWidth();

    int getColor();
}
