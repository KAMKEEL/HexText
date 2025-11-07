package kamkeel.hextext.client.render;

import kamkeel.hextext.api.rendering.HighlightSpan;
import kamkeel.hextext.api.rendering.TokenHighlightService;
import kamkeel.hextext.common.render.HighlightComputations;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.MathHelper;

import java.util.List;

/**
 * Utility helpers for computing and drawing token highlights when rendering raw text.
 */
public final class TokenHighlightUtils {

    private TokenHighlightUtils() {
    }

    public static float measureLiteralWidth(TokenHighlightService.WidthProvider provider, CharSequence text, int start,
                                            int length) {
        return HighlightComputations.measureLiteralWidth(provider, text, start, length);
    }

    public static float measureLiteralWidth(FontRenderer renderer, CharSequence text, int start, int length) {
        return measureLiteralWidth(renderer == null ? null : renderer::getStringWidth, text, start, length);
    }

    public static int getTokenHighlightColor(CharSequence text, int index) {
        return HighlightComputations.getTokenHighlightColor(text, index);
    }

    public static void drawHighlights(List<? extends HighlightSpan> highlights, int fontHeight) {
        if (highlights == null || highlights.isEmpty()) {
            return;
        }
        for (HighlightSpan highlight : highlights) {
            drawHighlight(highlight, fontHeight);
        }
    }

    private static void drawHighlight(HighlightSpan highlight, int fontHeight) {
        float left = highlight.getX();
        float right = highlight.getX() + highlight.getWidth();
        float top = highlight.getY() - 1.0f;
        float bottom = highlight.getY() + fontHeight;

        int x1 = MathHelper.floor_float(left);
        int y1 = MathHelper.floor_float(top);
        int x2 = MathHelper.ceiling_float_int(right);
        int y2 = MathHelper.ceiling_float_int(bottom);

        Gui.drawRect(x1, y1, x2, y2, highlight.getColor());
    }
}
