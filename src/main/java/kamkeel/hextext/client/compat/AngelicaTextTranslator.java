package kamkeel.hextext.client.compat;

import kamkeel.hextext.client.render.FontRenderContext;
import kamkeel.hextext.common.util.ColorCodeUtils;
import kamkeel.hextext.common.util.TextEffectMath;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Rewrites HexText tokens into the section-sign grammar Angelica's batching renderer reads.
 * Angelica owns the glyph loop, so HexText cannot apply render directives per character;
 * translating before Angelica parses keeps colours, spans and effects working.
 * <p>
 * {@code &#RRGGBB} and {@code <RRGGBB>} spans become {@code §x}; {@code &g} rainbow becomes
 * {@code §q}; {@code &h} becomes {@code §v}; {@code &i}/{@code &j} become {@code §i}/{@code §j}
 * where registered. Gradients are expanded per glyph so a stock Angelica draws HexText's ramp.
 * Colours cancel dynamic effects, so closing toggles are emitted ahead of them. Already-Angelica
 * grammar passes through, making translation safe to repeat.
 */
public final class AngelicaTextTranslator {

    private static final char SECTION = '§';
    private static final String VANILLA_COLOR_CODES = "0123456789abcdef";

    /** {@code §x} plus six {@code §<digit>} pairs. */
    private static final int SECTION_X_TOKEN_LENGTH = 14;
    /** {@code §g} plus two full {@code §x} tokens. */
    private static final int GRADIENT_TOKEN_LENGTH = 30;

    /** Colour-stack marker for the caller-supplied base colour. */
    private static final int COLOR_BASE = -1;

    private static String lastInput;
    private static String lastOutput;
    private static boolean lastGlyphEffects;
    private static boolean lastRaw;
    private static boolean lastAmpersandCodes;

    private AngelicaTextTranslator() {
    }

    /**
     * Spells Angelica's own {@code &q}/{@code &v} into section form when sending.
     * <p>
     * They are not HexText codes, so the ordinary send conversion leaves them, and
     * the render-side conversion is suppressed wherever the server has not allowed
     * ampersand formatting - the code then arrives as dead text. Escaped pairs pass
     * untouched, as in the main send conversion.
     */
    public static String convertAngelicaSendCodes(String text) {
        if (text == null || text.length() < 2 || text.indexOf('&') == -1) {
            return text;
        }
        StringBuilder out = null;
        int i = 0;
        while (i < text.length()) {
            char current = text.charAt(i);
            if (current == '&' && i + 1 < text.length()) {
                char lower = Character.toLowerCase(text.charAt(i + 1));
                if ((lower == 'q' || lower == 'v')
                    && (i == 0 || text.charAt(i - 1) != '\\')) {
                    if (out == null) {
                        out = new StringBuilder(text.length());
                        out.append(text, 0, i);
                    }
                    out.append(SECTION).append(text.charAt(i + 1));
                    i += 2;
                    continue;
                }
            }
            if (out != null) {
                out.append(current);
            }
            i++;
        }
        return out == null ? text : out.toString();
    }

    public static String translate(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        boolean raw = FontRenderContext.isRawTextRendering();

        if (text.indexOf('&') == -1 && text.indexOf('<') == -1 && text.indexOf(SECTION) == -1) {
            return text;
        }

        boolean glyphEffects = AngelicaClientCompat.areGlyphEffectsRegistered();
        boolean ampersandCodes = AngelicaClientCompat.convertsAmpersandCodes();
        // Every flag the output depends on is in the key: the chat line draws raw while
        // the history draws the same string normally.
        if (text == lastInput && glyphEffects == lastGlyphEffects && raw == lastRaw
            && ampersandCodes == lastAmpersandCodes) {
            return lastOutput;
        }

        String translated = translateImpl(text, glyphEffects, raw);
        lastInput = text;
        lastOutput = translated;
        lastGlyphEffects = glyphEffects;
        lastRaw = raw;
        lastAmpersandCodes = ampersandCodes;
        return translated;
    }

    /**
     * @param raw drawing text being edited, where the codes are part of what the reader
     *            needs to see. Every token is emitted twice: the literal characters that
     *            were typed, then the token that styles what follows.
     */
    private static String translateImpl(String text, boolean glyphEffects, boolean raw) {
        // An editor reads ampersands as codes whatever the config says: the line being
        // typed is on its way to a converter that will make them section signs.
        ColorCodeUtils.FormattingEnvironment env = ColorCodeUtils.captureFormattingEnvironment(raw);
        boolean allowHtml = env.allowsHtmlFormatting();
        boolean allowAmpersand = env.allowsUniversalAmpersand();

        int length = text.length();
        // Raw always diverges from the input, so the builder starts empty and every
        // character reaches it through the loop rather than through ensureOutput.
        StringBuilder out = raw ? new StringBuilder(text.length() + 24) : null;
        Deque<Integer> colorStack = null;
        StringBuilder activeStyles = null;
        int currentColor = COLOR_BASE;
        boolean waveActive = false;
        boolean dinnerboneActive = false;
        // Expanded here rather than handed to Angelica's own gradient: every glyph
        // gets its colour from HexText's ramp, so a stock Angelica draws what the
        // native renderer draws.
        boolean gradientActive = false;
        int gradientStartRgb = 0;
        int gradientEndRgb = 0;
        int gradientSpan = 0;
        int gradientIndex = 0;

        for (int i = 0; i < length; i++) {
            char current = text.charAt(i);

            // Escaped: Angelica reads the pair itself and draws a bare ampersand, so
            // both characters pass untouched and no branch below sees them. Reading
            // the code here ate the ampersand and stranded the backslash.
            if (current == '\\' && i + 1 < length) {
                char escaped = text.charAt(i + 1);
                if (escaped == '&' || escaped == SECTION) {
                    if (out != null) {
                        out.append(current).append(escaped);
                    }
                    i++;
                    continue;
                }
            }

            // Emitted before the branches, which each consume their own token. Only
            // where a branch will match, so a meaningless ampersand stays text.
            if (raw) {
                int spelt = tokenLength(text, i, allowHtml, allowAmpersand);
                if (spelt > 0) {
                    appendLiteral(out, text, i, spelt);
                }
            }

            if ((current == SECTION || (current == '&' && allowAmpersand)) && i + 1 < length) {
                char next = text.charAt(i + 1);
                char lower = Character.toLowerCase(next);

                if (next == '#' && ColorCodeUtils.isValidHexString(text, i + 2)) {
                    int rgb = ColorCodeUtils.parseHexColor(text, i + 2);
                    if (rgb != -1) {
                        out = ensureOutput(out, text, i);
                        // Inline hex resets styles in HexText; §x alone would carry them across.
                        if (activeStyles != null && activeStyles.length() > 0) {
                            out.append(SECTION).append('r');
                            activeStyles.setLength(0);
                            waveActive = false;
                            dinnerboneActive = false;
                        } else {
                            waveActive = closeWave(out, waveActive);
                            dinnerboneActive = closeDinnerbone(out, dinnerboneActive);
                        }
                        appendSectionX(out, rgb);
                        if (colorStack != null) {
                            colorStack.clear();
                        }
                        currentColor = rgb;
                        gradientActive = false;
                        i += 7;
                        continue;
                    }
                }

                if (current == SECTION && lower == 'x') {
                    int rgb = parseSectionX(text, i);
                    if (rgb != -1) {
                        if (out != null) {
                            out.append(text, i, i + SECTION_X_TOKEN_LENGTH);
                        }
                        currentColor = rgb;
                        gradientActive = false;
                        i += SECTION_X_TOKEN_LENGTH - 1;
                        continue;
                    }
                }

                // Angelica's own effects, either spelling. The suppressor holds off
                // Angelica's conversion in an editor, so HexText reads them itself.
                if (lower == 'u' && (current == SECTION || allowAmpersand)) {
                    // Marker only; a colour after it is its own token next turn, which
                    // is what lets §u§x.. and &u&#.. both arrive as a custom shadow.
                    out = ensureOutput(out, text, i);
                    out.append(SECTION).append('u');
                    i++;
                    continue;
                }

                // z is HexText's own wave, read wherever it appears. q and v are
                // Angelica's spellings of HexText's g and h, so outside an editor they
                // stay text for Angelica to convert; an editor previews them only when
                // that conversion is actually going to happen.
                if ((lower == 'z' && (current == SECTION || allowAmpersand))
                    || ((lower == 'q' || lower == 'v')
                        && (current == SECTION
                            || (raw && AngelicaClientCompat.convertsAmpersandCodes())))) {
                    if (lower == 'z') {
                        waveActive = !waveActive;
                    } else if (lower == 'v') {
                        dinnerboneActive = !dinnerboneActive;
                    }
                    out = ensureOutput(out, text, i);
                    out.append(SECTION).append(lower);
                    i++;
                    continue;
                }

                if (lower == 'g') {
                    if (current == SECTION && parseSectionX(text, i + 2) != -1
                        && parseSectionX(text, i + 2 + SECTION_X_TOKEN_LENGTH) != -1) {
                        out = ensureOutput(out, text, i);
                        gradientStartRgb = parseSectionX(text, i + 2);
                        gradientEndRgb = parseSectionX(text, i + 2 + SECTION_X_TOKEN_LENGTH);
                        gradientSpan = countVisibleGlyphs(text, i + GRADIENT_TOKEN_LENGTH, allowHtml, allowAmpersand);
                        gradientIndex = 0;
                        gradientActive = true;
                        i += GRADIENT_TOKEN_LENGTH - 1;
                        continue;
                    }
                    if (isHexGradient(text, i)) {
                        out = ensureOutput(out, text, i);
                        gradientStartRgb = ColorCodeUtils.parseHexColor(text, i + 4);
                        gradientEndRgb = ColorCodeUtils.parseHexColor(text, i + 12);
                        gradientSpan = countVisibleGlyphs(text, i + 18, allowHtml, allowAmpersand);
                        gradientIndex = 0;
                        gradientActive = true;
                        i += 17;
                        continue;
                    }
                    // HexText's own rainbow where the registry took it; §q is a fixed
                    // table and never animates, so it is only the fallback.
                    out = ensureOutput(out, text, i);
                    out.append(SECTION).append(AngelicaClientCompat.isRainbowRegistered()
                        ? AngelicaClientCompat.rainbowCode() : 'q');
                    gradientActive = false;
                    i++;
                    continue;
                }

                if (lower == 'h' || lower == 'j') {
                    out = ensureOutput(out, text, i);
                    if (lower == 'h' && !dinnerboneActive) {
                        out.append(SECTION).append('v');
                        dinnerboneActive = true;
                    } else if (lower == 'j') {
                        if (glyphEffects) {
                            out.append(SECTION).append('j');
                        } else if (!waveActive) {
                            out.append(SECTION).append('z');
                            waveActive = true;
                        }
                    }
                    i++;
                    continue;
                }

                if (lower == 'i') {
                    out = ensureOutput(out, text, i);
                    if (glyphEffects) {
                        out.append(SECTION).append('i');
                    }
                    i++;
                    continue;
                }

                if (ColorCodeUtils.isMinecraftColorCode(lower)) {
                    if (current == '&' || waveActive || dinnerboneActive) {
                        out = ensureOutput(out, text, i);
                    }
                    if (out != null) {
                        waveActive = closeWave(out, waveActive);
                        dinnerboneActive = closeDinnerbone(out, dinnerboneActive);
                        out.append(SECTION).append(next);
                    }
                    if (colorStack != null) {
                        colorStack.clear();
                    }
                    if (activeStyles != null) {
                        activeStyles.setLength(0);
                    }
                    currentColor = encodeVanillaColor(ColorCodeUtils.getMinecraftColorIndex(lower));
                    gradientActive = false;
                    i++;
                    continue;
                }

                if (ColorCodeUtils.isResetCode(lower)) {
                    if (current == '&') {
                        out = ensureOutput(out, text, i);
                    }
                    if (out != null) {
                        out.append(SECTION).append(next);
                    }
                    if (colorStack != null) {
                        colorStack.clear();
                    }
                    if (activeStyles != null) {
                        activeStyles.setLength(0);
                    }
                    currentColor = COLOR_BASE;
                    waveActive = false;
                    dinnerboneActive = false;
                    gradientActive = false;
                    i++;
                    continue;
                }

                if (ColorCodeUtils.isStyleCode(lower)) {
                    if (current == '&') {
                        out = ensureOutput(out, text, i);
                    }
                    if (out != null) {
                        out.append(SECTION).append(next);
                    }
                    if (activeStyles == null) {
                        activeStyles = new StringBuilder(5);
                    }
                    if (activeStyles.indexOf(String.valueOf(lower)) == -1) {
                        activeStyles.append(lower);
                    }
                    i++;
                    continue;
                }
            }

            if (allowHtml && current == '<') {
                if (i + 8 <= length && text.charAt(i + 7) == '>' && ColorCodeUtils.isValidHexString(text, i + 1)) {
                    int rgb = ColorCodeUtils.parseHexColor(text, i + 1);
                    if (rgb != -1) {
                        out = ensureOutput(out, text, i);
                        // Opening a span resets styles like an inline hex does; only the
                        // closing tag preserves them.
                        if (activeStyles != null && activeStyles.length() > 0) {
                            out.append(SECTION).append('r');
                            activeStyles.setLength(0);
                            waveActive = false;
                            dinnerboneActive = false;
                        } else {
                            waveActive = closeWave(out, waveActive);
                            dinnerboneActive = closeDinnerbone(out, dinnerboneActive);
                        }
                        if (colorStack == null) {
                            colorStack = new ArrayDeque<>();
                        }
                        colorStack.push(currentColor);
                        appendSectionX(out, rgb);
                        currentColor = rgb;
                        gradientActive = false;
                        i += 7;
                        continue;
                    }
                }

                if (i + 9 <= length && text.charAt(i + 1) == '/' && text.charAt(i + 8) == '>'
                    && ColorCodeUtils.isValidHexString(text, i + 2)) {
                    out = ensureOutput(out, text, i);
                    waveActive = closeWave(out, waveActive);
                    dinnerboneActive = closeDinnerbone(out, dinnerboneActive);
                    int restored = colorStack == null || colorStack.isEmpty() ? COLOR_BASE : colorStack.pop();
                    appendRestoredColor(out, restored, activeStyles);
                    currentColor = restored;
                    gradientActive = false;
                    i += 8;
                    continue;
                }
            }

            if (out != null) {
                if (gradientActive && current != '\n') {
                    appendSectionX(out, TextEffectMath.computeGradientColor(
                        gradientStartRgb, gradientEndRgb, gradientIndex++, gradientSpan));
                }
                out.append(current);
            }
        }

        return out != null ? out.toString() : text;
    }

    /**
     * How many characters the branches below consume as one token, or zero. Mirrors
     * their conditions and must agree exactly: too short drops typed characters, too
     * long repeats them. The parity test checks every form against the source.
     */
    private static int tokenLength(String text, int i, boolean allowHtml, boolean allowAmpersand) {
        int length = text.length();
        char current = text.charAt(i);

        if (allowHtml && current == '<') {
            if (i + 9 <= length && text.charAt(i + 1) == '/' && text.charAt(i + 8) == '>'
                && ColorCodeUtils.isValidHexString(text, i + 2)) {
                return 9;
            }
            if (i + 8 <= length && text.charAt(i + 7) == '>'
                && ColorCodeUtils.isValidHexString(text, i + 1)) {
                return 8;
            }
            return 0;
        }

        if ((current != SECTION && !(current == '&' && allowAmpersand)) || i + 1 >= length) {
            return 0;
        }

        char next = text.charAt(i + 1);
        char lower = Character.toLowerCase(next);

        if (next == '#' && ColorCodeUtils.isValidHexString(text, i + 2)
            && ColorCodeUtils.parseHexColor(text, i + 2) != -1) {
            return 8;
        }
        if (current == SECTION && lower == 'x') {
            return parseSectionX(text, i) != -1 ? SECTION_X_TOKEN_LENGTH : 0;
        }
        if (lower == 'u' || lower == 'z') {
            return 2;
        }
        if (lower == 'q' || lower == 'v') {
            // This is only ever asked in raw mode, so the branch it mirrors consumes
            // these exactly when Angelica is the one that would have converted them.
            // Claiming a length the branch will not take repeats the characters.
            return current == SECTION || AngelicaClientCompat.convertsAmpersandCodes() ? 2 : 0;
        }
        if (lower == 'g') {
            if (current == SECTION && parseSectionX(text, i + 2) != -1
                && parseSectionX(text, i + 2 + SECTION_X_TOKEN_LENGTH) != -1) {
                return GRADIENT_TOKEN_LENGTH;
            }
            if (isHexGradient(text, i)) {
                return 18;
            }
            return 2;
        }
        if (lower == 'h' || lower == 'i' || lower == 'j') {
            return 2;
        }
        if (ColorCodeUtils.isMinecraftColorCode(lower)
            || ColorCodeUtils.isResetCode(lower)
            || ColorCodeUtils.isStyleCode(lower)) {
            return 2;
        }
        return 0;
    }

    /**
     * The token as typed, always spelled with ampersands - a section sign would be
     * parsed as the very code this is showing. The suppressor keeps them as glyphs.
     */
    private static void appendLiteral(StringBuilder out, String text, int from, int count) {
        // A colour token wears its own colour, so the editor shows what each token
        // will do. Styles and effects have no colour to show and keep the running
        // one. Left open deliberately: the branch that follows re-emits the same
        // colour as its real directive.
        appendLiteralColor(out, text, from, count);

        // A toggle, not a latch: a latched effect could only end by resetting the
        // colour the token just set. Absent when Angelica refused the code.
        boolean wash = AngelicaClientCompat.isHighlightRegistered();
        if (wash) out.append(SECTION).append(AngelicaClientCompat.highlightCode());
        for (int index = from; index < from + count && index < text.length(); index++) {
            char character = text.charAt(index);
            out.append(character == SECTION ? '&' : character);
        }
        if (wash) out.append(SECTION).append(AngelicaClientCompat.highlightCode());
    }

    private static void appendLiteralColor(StringBuilder out, String text, int from, int count) {
        char first = text.charAt(from);

        if (count == 2 && (first == '&' || first == SECTION)) {
            char code = Character.toLowerCase(text.charAt(from + 1));
            // Styles as well as colours: the native renderer applies a code to its own
            // characters, and the width walkers measure them that way, so a token that
            // did not style itself here left the cursor short by its own bold.
            if (ColorCodeUtils.isMinecraftColorCode(code) || ColorCodeUtils.isStyleCode(code)) {
                out.append(SECTION).append(code);
            }
            return;
        }

        // Inline hex, either spelling: the token names its own colour.
        if (count == 8 && (first == '&' || first == SECTION) && text.charAt(from + 1) == '#') {
            int rgb = ColorCodeUtils.parseHexColor(text, from + 2);
            if (rgb != -1) {
                appendSectionX(out, rgb);
            }
            return;
        }

        // Span open and close both carry a hex to show.
        if (first == '<') {
            int hexStart = count == 9 ? from + 2 : from + 1;
            int rgb = ColorCodeUtils.parseHexColor(text, hexStart);
            if (rgb != -1) {
                appendSectionX(out, rgb);
            }
            return;
        }

        // A §x token colours itself.
        if (count == SECTION_X_TOKEN_LENGTH && first == SECTION) {
            int rgb = parseSectionX(text, from);
            if (rgb != -1) {
                appendSectionX(out, rgb);
            }
            return;
        }

        // A gradient opens in its start colour, under either spelling.
        if (count == 18 && Character.toLowerCase(text.charAt(from + 1)) == 'g') {
            int rgb = ColorCodeUtils.parseHexColor(text, from + 4);
            if (rgb != -1) {
                appendSectionX(out, rgb);
            }
            return;
        }
        if (count == GRADIENT_TOKEN_LENGTH && Character.toLowerCase(text.charAt(from + 1)) == 'g') {
            int rgb = parseSectionX(text, from + 2);
            if (rgb != -1) {
                appendSectionX(out, rgb);
            }
        }
    }

    /**
     * Glyphs a gradient has to travel, counted as the branches consume tokens: a
     * {@link #tokenLength} match is zero-width, everything else is a glyph. Stops at
     * the first token carrying a colour, so a ramp cut short still reaches its end
     * colour. The native renderer and the wrap carry count the same way.
     */
    private static int countVisibleGlyphs(String text, int from, boolean allowHtml, boolean allowAmpersand) {
        int visible = 0;
        for (int i = from; i < text.length(); ) {
            int token = tokenLength(text, i, allowHtml, allowAmpersand);
            if (token > 0) {
                if (endsGradient(text, i, token)) {
                    return visible;
                }
                i += token;
                continue;
            }
            if (text.charAt(i) != '\n') {
                visible++;
            }
            i++;
        }
        return visible;
    }

    /** Whether a token carries a colour of its own, which is where a ramp stops. */
    private static boolean endsGradient(String text, int at, int tokenLen) {
        if (tokenLen != 2) {
            // Hex under either spelling, §x forms, both span tags, and full gradient
            // tokens all set a colour.
            return true;
        }
        char code = Character.toLowerCase(text.charAt(at + 1));
        return ColorCodeUtils.isMinecraftColorCode(code) || ColorCodeUtils.isResetCode(code)
            || code == 'g' || code == 'q';
    }

    private static StringBuilder ensureOutput(StringBuilder out, String text, int upTo) {
        if (out == null) {
            out = new StringBuilder(text.length() + 24);
            out.append(text, 0, upTo);
        }
        return out;
    }

    /**
     * Re-applies the colour a closing span restores. Vanilla colours and {@code §r} clear
     * Angelica's style flags, so styles are replayed after them; {@code §x} keeps them.
     */
    private static void appendRestoredColor(StringBuilder out, int restored, StringBuilder activeStyles) {
        if (restored == COLOR_BASE) {
            out.append(SECTION).append('r');
            appendStyles(out, activeStyles);
        } else if (restored < COLOR_BASE) {
            out.append(SECTION).append(VANILLA_COLOR_CODES.charAt(decodeVanillaColor(restored)));
            appendStyles(out, activeStyles);
        } else {
            appendSectionX(out, restored);
        }
    }

    private static void appendStyles(StringBuilder out, StringBuilder activeStyles) {
        if (activeStyles == null) {
            return;
        }
        for (int i = 0; i < activeStyles.length(); i++) {
            out.append(SECTION).append(activeStyles.charAt(i));
        }
    }

    private static void appendSectionX(StringBuilder out, int rgb) {
        out.append(SECTION).append('x');
        for (int shift = 20; shift >= 0; shift -= 4) {
            out.append(SECTION).append(Character.forDigit((rgb >> shift) & 0xF, 16));
        }
    }

    private static boolean closeWave(StringBuilder out, boolean waveActive) {
        if (waveActive) {
            out.append(SECTION).append('z');
        }
        return false;
    }

    private static boolean closeDinnerbone(StringBuilder out, boolean dinnerboneActive) {
        if (dinnerboneActive) {
            out.append(SECTION).append('v');
        }
        return false;
    }

    private static int parseSectionX(String text, int start) {
        if (start < 0 || start + SECTION_X_TOKEN_LENGTH > text.length()) {
            return -1;
        }
        if (text.charAt(start) != SECTION || Character.toLowerCase(text.charAt(start + 1)) != 'x') {
            return -1;
        }
        int rgb = 0;
        for (int pos = start + 2; pos < start + SECTION_X_TOKEN_LENGTH; pos += 2) {
            if (text.charAt(pos) != SECTION) {
                return -1;
            }
            int digit = Character.digit(text.charAt(pos + 1), 16);
            if (digit == -1) {
                return -1;
            }
            rgb = (rgb << 4) | digit;
        }
        return rgb;
    }

    /**
     * A gradient written as two inline hex colours. Each marker is taken as {@code &} or
     * {@code §} independently: the send conversion rewrites ampersands, so the same
     * gradient arrives spelled differently in the chat history.
     */
    private static boolean isHexGradient(String text, int start) {
        return start + 18 <= text.length()
            && isHexMarker(text, start + 2) && ColorCodeUtils.isValidHexString(text, start + 4)
            && isHexMarker(text, start + 10) && ColorCodeUtils.isValidHexString(text, start + 12);
    }

    private static boolean isHexMarker(String text, int at) {
        char marker = text.charAt(at);
        return (marker == '&' || marker == SECTION) && text.charAt(at + 1) == '#';
    }

    private static int encodeVanillaColor(int colorIndex) {
        return -(colorIndex + 2);
    }

    private static int decodeVanillaColor(int encoded) {
        return -encoded - 2;
    }
}
