package kamkeel.hextext.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ColorMathTest {

    @Test
    public void scaleBrightnessDarkensColour() {
        int darkened = ColorMath.scaleBrightness(0xFF8040, 0.5f);
        assertEquals(0x804020, darkened);
    }

    @Test
    public void scaleBrightnessClampsWhenBrightening() {
        int brightened = ColorMath.scaleBrightness(0x808080, 3.0f);
        assertEquals(0xFFFFFF, brightened);
    }

    @Test
    public void blendInterpolatesBetweenColours() {
        int blended = ColorMath.blend(0x000000, 0xFFFFFF, 0.5f);
        assertEquals(0x808080, blended);
    }
}
