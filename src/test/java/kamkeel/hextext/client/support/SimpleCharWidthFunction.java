package kamkeel.hextext.client.support;

import java.util.HashMap;
import java.util.Map;

import kamkeel.hextext.client.FormattedTextMetrics;

public final class SimpleCharWidthFunction implements FormattedTextMetrics.CharWidthFunction {

    private final float defaultWidth;
    private final Map<Character, Float> overrides = new HashMap<Character, Float>();

    public SimpleCharWidthFunction(float defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    public void setWidth(char character, float width) {
        overrides.put(Character.valueOf(character), Float.valueOf(width));
    }

    @Override
    public float getWidth(char character) {
        Float override = overrides.get(Character.valueOf(character));
        if (override != null) {
            return override.floatValue();
        }
        return defaultWidth;
    }
}
