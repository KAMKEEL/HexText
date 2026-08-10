package kamkeel.hextext.common.util;

/**
 * Carries a gradient across a line break so it ramps over the whole message.
 *
 * <p>The renderer spreads a gradient over the visible glyphs of the one string it
 * is handed, and chat hands it each wrapped line separately - so the first line
 * ramped from start to end all by itself and every later line arrived flat in the
 * end colour. The ramp has to be split where the text is split: the first line's
 * token is rewritten to stop at the colour the ramp has reached by the break, and
 * the continuation opens with a token that carries on from there to the original
 * end. Each recursion of the wrap does this again, so a message that wraps three
 * times becomes three tokens drawing one straight ramp.</p>
 *
 * <p>The boundary colour comes from {@link TextEffectMath#computeGradientColor},
 * the same function the renderer asks per glyph, which is what makes the last
 * glyph of one line and the first of the next sit on the same ramp.</p>
 */
public final class GradientWrap {

    private GradientWrap() {
    }

    /** What {@link #carryAcrossBreak} found, all offsets already applied. */
    public static final class Carry {
        /** The first line with its token's end colour pulled back to the break. */
        public final String rewrittenFirstPart;
        /** The rewritten token itself, as it now appears inside the first line. */
        public final String rewrittenToken;
        /** The token that continues the ramp on the next line. */
        public final String continuationToken;

        private Carry(String rewrittenFirstPart, String rewrittenToken, String continuationToken) {
            this.rewrittenFirstPart = rewrittenFirstPart;
            this.rewrittenToken = rewrittenToken;
            this.continuationToken = continuationToken;
        }
    }

    /**
     * Splits the ramp of a gradient still active at the end of {@code firstPart},
     * or returns {@code null} when there is nothing to carry.
     *
     * @param firstPart the wrapped-off first line
     * @param rest      everything after the break, before any prefix is added
     */
    public static Carry carryAcrossBreak(String firstPart, String rest) {
        int token = findActiveGradient(firstPart);
        if (token == -1 || rest == null || rest.isEmpty()) {
            return null;
        }

        int startRgb = ColorCodeUtils.parseHexColor(firstPart, token + 4);
        int endRgb = ColorCodeUtils.parseHexColor(firstPart, token + 12);
        if (startRgb == -1 || endRgb == -1) {
            return null;
        }

        // Glyphs are counted the way the renderer counts a span: every non-code
        // character, to the end of the segment. Terminators are deliberately NOT
        // stopped at - the renderer stretches a span past them too, and matching
        // that quirk is what keeps the per-line ramps on one line's ramp.
        int visibleFirst = countVisibleGlyphs(firstPart, token + ColorCodeUtils.GRADIENT_TOKEN_LENGTH);
        int visibleRest = countVisibleGlyphs(rest, 0);
        if (visibleFirst == 0 || visibleRest == 0) {
            return null;
        }

        int boundaryRgb = TextEffectMath.computeGradientColor(startRgb, endRgb, visibleFirst,
            visibleFirst + visibleRest);
        String boundaryHex = String.format("%06x", boundaryRgb & 0xFFFFFF);
        String originalEndHex = firstPart.substring(token + 12, token + 18);

        StringBuilder rewrittenFirst = new StringBuilder(firstPart);
        for (int i = 0; i < 6; i++) {
            rewrittenFirst.setCharAt(token + 12 + i, boundaryHex.charAt(i));
        }
        String rewrittenToken = rewrittenFirst.substring(token, token + ColorCodeUtils.GRADIENT_TOKEN_LENGTH);

        // Spelled with the same markers as the original, so an ampersand gradient
        // stays an ampersand gradient and survives whatever conversion the string
        // is about to go through.
        StringBuilder continuation = new StringBuilder(ColorCodeUtils.GRADIENT_TOKEN_LENGTH);
        continuation.append(firstPart.charAt(token)).append(firstPart.charAt(token + 1));
        continuation.append(firstPart.charAt(token + 2)).append('#').append(boundaryHex);
        continuation.append(firstPart.charAt(token + 10)).append('#').append(originalEndHex);

        return new Carry(rewrittenFirst.toString(), rewrittenToken, continuation.toString());
    }

    /** The start of the last gradient token no later colour has replaced, or -1. */
    private static int findActiveGradient(String text) {
        int active = -1;
        for (int i = 0; i < text.length(); ) {
            int gradient = ColorCodeUtils.gradientTokenLength(text, i);
            if (gradient > 0) {
                active = i;
                i += gradient;
                continue;
            }
            int code = ColorCodeUtils.detectColorCodeLength(text, i);
            if (code == 0) {
                i++;
                continue;
            }
            if (replacesGradient(text, i, code)) {
                active = -1;
            }
            i += code;
        }
        return active;
    }

    /** Whether this code carries a colour of its own, which ends a running ramp. */
    private static boolean replacesGradient(CharSequence text, int at, int codeLength) {
        if (codeLength != 2) {
            // Inline hex under either spelling, both span tags, and §x forms.
            return true;
        }
        char code = Character.toLowerCase(text.charAt(at + 1));
        return ColorCodeUtils.isMinecraftColorCode(code) || ColorCodeUtils.isResetCode(code)
            || code == 'g';
    }

    private static int countVisibleGlyphs(CharSequence text, int from) {
        int visible = 0;
        for (int i = from; i < text.length(); ) {
            int gradient = ColorCodeUtils.gradientTokenLength(text, i);
            if (gradient > 0) {
                i += gradient;
                continue;
            }
            int code = ColorCodeUtils.detectColorCodeLength(text, i);
            if (code > 0) {
                i += code;
                continue;
            }
            if (text.charAt(i) != '\n') {
                visible++;
            }
            i++;
        }
        return visible;
    }
}
