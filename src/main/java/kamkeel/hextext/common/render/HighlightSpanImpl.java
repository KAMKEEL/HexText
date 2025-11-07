package kamkeel.hextext.common.render;

import kamkeel.hextext.api.rendering.HighlightSpan;

public class HighlightSpanImpl implements HighlightSpan {
    private final float x;
    private final float y;
    private final float width;
    private final int color;

    public HighlightSpanImpl(float x, float y, float width, int color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.color = color;
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public int getColor() {
        return color;
    }
}
