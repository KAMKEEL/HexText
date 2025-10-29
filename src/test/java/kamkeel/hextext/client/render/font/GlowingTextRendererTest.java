package kamkeel.hextext.client.render.font;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GlowingTextRendererTest {

    @Test
    public void outlineTurnsBlackTextWhite() {
        int outline = GlowingTextRenderer.computeOutlineColor(0x000000);
        assertEquals(0xFFFFFF, outline);
    }

    @Test
    public void outlineTreatsBlackWithAlphaAsBlack() {
        int outline = GlowingTextRenderer.computeOutlineColor(0xFF000000);
        assertEquals(0xFFFFFF, outline);
    }

    @Test
    public void outlineDarkensColouredText() {
        int baseColor = 0x3366CC;
        int outline = GlowingTextRenderer.computeOutlineColor(baseColor);
        assertEquals(0x142952, outline);
    }

    @Test
    public void outlineRecoversWhenDarkeningRemovesAllColour() {
        int baseColor = 0x010101;
        int outline = GlowingTextRenderer.computeOutlineColor(baseColor);
        assertEquals(0x000000, outline);
    }
}
