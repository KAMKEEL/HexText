package kamkeel.hextext.client.render;

import kamkeel.hextext.common.util.ColorCodeUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.MathHelper;

import java.util.List;

/**
 * Utility helpers for computing and drawing token highlights when rendering raw text.
 */
public final class TokenHighlightUtils {

    private TokenHighlightUtils() {}

    public static float measureLiteralWidth(FontRenderer renderer, CharSequence text, int start, int length) {
        if (renderer == null || text == null || length <= 0 || start < 0 || start >= text.length()) {
            return 0.0f;
        }
        int end = Math.min(start + length, text.length());
        if (end <= start) {
            return 0.0f;
        }
        String segment = text.subSequence(start, end).toString();
        return renderer.getStringWidth(segment);
    }

    public static int getTokenHighlightColor(CharSequence text, int index) {
        char c = text.charAt(index);
        if (c == 167 || (c == '&' && index + 1 < text.length()
            && ColorCodeUtils.isFormattingCode(text.charAt(index + 1)))) {
            return 0x304080FF;
        }
        if (c == '&') {
            return 0x3039C86F;
        }
        if (c == '<') {
            if (index + 1 < text.length() && text.charAt(index + 1) == '/') {
                return 0x30FF8C5A;
            }
            return 0x305A8CFF;
        }
        return 0x30222222;
    }

    public static void drawHighlights(List<TokenHighlight> highlights, int fontHeight) {
        if (highlights == null || highlights.isEmpty()) {
            return;
        }
        for (TokenHighlight highlight : highlights) {
            drawHighlight(highlight, fontHeight);
        }
    }

    private static void drawHighlight(TokenHighlight highlight, int fontHeight) {
        float left = highlight.x;
        float right = highlight.x + highlight.width;
        float top = highlight.y - 1.0f;
        float bottom = highlight.y + fontHeight;

        int x1 = MathHelper.floor_float(left);
        int y1 = MathHelper.floor_float(top);
        int x2 = MathHelper.ceiling_float_int(right);
        int y2 = MathHelper.ceiling_float_int(bottom);

        Gui.drawRect(x1, y1, x2, y2, highlight.color);
    }
}
