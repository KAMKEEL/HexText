package kamkeel.hextext.common.render;

import kamkeel.hextext.api.rendering.TokenHighlightService;
import kamkeel.hextext.common.util.ColorCodeUtils;

public final class HighlightComputations {

    private HighlightComputations() {
    }

    public static float measureLiteralWidth(TokenHighlightService.WidthProvider provider, CharSequence text, int start,
                                            int length) {
        if (provider == null || text == null || length <= 0 || start < 0 || start >= text.length()) {
            return 0.0f;
        }
        int end = Math.min(start + length, text.length());
        if (end <= start) {
            return 0.0f;
        }
        String segment = text.subSequence(start, end).toString();
        return provider.getStringWidth(segment);
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
}
