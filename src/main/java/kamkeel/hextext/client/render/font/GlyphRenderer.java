package kamkeel.hextext.client.render.font;

public interface GlyphRenderer {
    float renderDefault(int glyphIndex, boolean italic);

    float renderUnicode(char character, boolean italic);
}
