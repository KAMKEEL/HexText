package kamkeel.hextext.client.compat;

import kamkeel.hextext.client.render.FontRenderContext;
import kamkeel.hextext.common.util.ColorCodeUtils;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Rewrites HexText formatting tokens into the section-sign grammar understood by Angelica's
 * batching font renderer. When Angelica owns the font pipeline the vanilla character loop never
 * runs, so HexText cannot apply its render directives per glyph; translating the text before
 * Angelica parses it keeps colours, spans and effects working without any render hooks.
 *
 * <p>Token mapping:
 * <ul>
 * <li>{@code &#RRGGBB} / {@code §#RRGGBB} become {@code §x§R§R§G§G§B§B}</li>
 * <li>{@code <RRGGBB>} becomes {@code §x…}; {@code </RRGGBB>} restores the enclosing colour</li>
 * <li>{@code &g} rainbow becomes {@code §q}; the Angelica gradient forms {@code &g&#..&#..} and
 * {@code §g§x..§x..} are preserved as gradients</li>
 * <li>{@code &h} dinnerbone becomes {@code §v}</li>
 * <li>{@code &i} ignite and {@code &j} shake become {@code §i}/{@code §j} when HexText's effects
 * are registered in Angelica's font effect registry; otherwise ignite is dropped and shake
 * becomes {@code §z} (wave)</li>
 * </ul>
 *
 * <p>Colour changes in HexText also cancel any active dynamic effect, so the translator emits
 * closing {@code §z}/{@code §v} toggles ahead of colour codes while those effects are active.
 * Strings that already use Angelica's grammar pass through unchanged, which makes the
 * translation safe to apply repeatedly.
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
        // Raw belongs in the key. The chat line draws its contents raw while the
        // history above it draws the same string normally, so one cache slot
        // keyed only on the text hands one of them the other's answer. Every other
        // flag the output turns on belongs there for the same reason.
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
     * @param raw whether the caller is drawing text that is being edited, where the
     *            codes themselves are part of what the reader needs to see. Every
     *            token is then emitted twice: once as the literal characters that
     *            were typed, and once as the section-sign token that styles what
     *            follows. HexText's own renderer does this by drawing the token's
     *            glyphs and applying its directive in the same pass; Angelica owns
     *            the glyph loop here, so the only way to say both things is to put
     *            both in the string. Without it the chat line showed the codes and
     *            no colour, which is half a live edit.
     */
    private static String translateImpl(String text, boolean glyphEffects, boolean raw) {
        // Raw is passed through, not hardcoded false. An editor is the one place an
        // ampersand is a code no matter what the config says: universal ampersand
        // formatting governs text the world has already produced, while the thing
        // being typed is on its way to a converter that will turn it into a section
        // sign the moment it is sent. Reading it as plain text meanwhile is what made
        // the chat line ignore every &-form code while colouring the same string
        // correctly once it arrived in the history above.
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

        for (int i = 0; i < length; i++) {
            char current = text.charAt(i);

            // A backslash spoken for the marker behind it. Angelica reads the pair
            // itself and draws a bare ampersand, so both characters go through
            // untouched and none of the branches below get a look at them. Reading
            // the code here instead consumed the ampersand and left the backslash
            // standing in front of a colour that was never meant to apply, which is
            // every escaped form rendering as a stray slash and a styled word.
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

            // The literal goes in before the branches run, because each of them
            // consumes its own token and there is no shared place afterwards to
            // put it. Emitted only where a branch will actually match, so an
            // ampersand that spells nothing is left to fall through as text.
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
                        // Inline hex resets styles and dynamic effects in HexText (APPLY_RGB has
                        // resetFormatting set); §x alone would carry styles across, so a reset is
                        // emitted first when any style is active.
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
                        i += SECTION_X_TOKEN_LENGTH - 1;
                        continue;
                    }
                }

                // Angelica's own effects, under either spelling. Outside an editor
                // Angelica converts the ampersand form itself, so these worked when
                // sent and did nothing while being typed - the suppressor holds that
                // conversion off precisely so HexText can show the code, and then
                // HexText has to be the one that knows what the code means.
                // u is HexText's own shadow tint now, so both renderers read it.
                if (lower == 'u' && (current == SECTION || allowAmpersand)) {
                    // Only the marker is emitted; a colour after it is a token in its
                    // own right and the next turn of the loop translates it, which is
                    // what lets §u§x.. and &u&#.. both arrive as a custom shadow.
                    out = ensureOutput(out, text, i);
                    out.append(SECTION).append('u');
                    i++;
                    continue;
                }

                // z is HexText's own wave now, so it is read wherever it appears -
                // the native renderer consumes it and these two must agree. q and v
                // are Angelica's spellings of codes HexText writes as g and h, so
                // outside an editor they stay text and Angelica converts them itself.
                // The editor previews them only when Angelica is actually going to
                // make that conversion; with it switched off the code stays text once
                // sent, and a preview that rainbowed anyway was promising something
                // the chat line would not deliver.
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
                        if (out != null) {
                            out.append(text, i, i + GRADIENT_TOKEN_LENGTH);
                        }
                        i += GRADIENT_TOKEN_LENGTH - 1;
                        continue;
                    }
                    if (isHexGradient(text, i)) {
                        out = ensureOutput(out, text, i);
                        out.append(SECTION).append('g');
                        appendSectionX(out, ColorCodeUtils.parseHexColor(text, i + 4));
                        appendSectionX(out, ColorCodeUtils.parseHexColor(text, i + 12));
                        i += 17;
                        continue;
                    }
                    // HexText's own rainbow where Angelica took it, because §q is a
                    // fixed table indexed by character and never moves; falling back
                    // to it only when the registry refused, where a still rainbow
                    // still beats no rainbow.
                    out = ensureOutput(out, text, i);
                    out.append(SECTION).append(AngelicaClientCompat.isRainbowRegistered()
                        ? AngelicaClientCompat.rainbowCode() : 'q');
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
                        // Opening a span resets styles and dynamic effects like an inline hex
                        // colour does (PUSH_RGB has resetFormatting set); only the closing tag
                        // preserves styles.
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
                    i += 8;
                    continue;
                }
            }

            if (out != null) {
                out.append(current);
            }
        }

        return out != null ? out.toString() : text;
    }

    /**
     * How many characters the branches below will consume as one token, or zero.
     *
     * <p>This mirrors their conditions rather than sharing them, because each
     * branch decides its own length as it emits. The two must agree exactly: a
     * literal shorter than what is consumed drops characters the reader typed, and
     * a literal longer than it repeats them. The parity test walks every form and
     * checks the literal against the source, which is what keeps the pair honest.</p>
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
     * The token as it was typed, in a spelling Angelica will not read back.
     *
     * <p>Always ampersands: a section sign here would be parsed as the very code
     * this is trying to show, and the characters would vanish into the thing they
     * are meant to be displaying. HexText holds Angelica's conversion suppressor
     * open for the whole of a raw draw, so the ampersands stay as glyphs.</p>
     */
    private static void appendLiteral(StringBuilder out, String text, int from, int count) {
        // Toggled on around the characters and off again, which is why the effect had
        // to become a toggle: a latched one could only end by resetting the colour it
        // sits in front of. When Angelica would not take the code the wash is simply
        // absent and the characters still read - the highlight says "this is a code",
        // it is not what makes it legible.
        boolean wash = AngelicaClientCompat.isHighlightRegistered();
        if (wash) out.append(SECTION).append(AngelicaClientCompat.highlightCode());
        for (int index = from; index < from + count && index < text.length(); index++) {
            char character = text.charAt(index);
            out.append(character == SECTION ? '&' : character);
        }
        if (wash) out.append(SECTION).append(AngelicaClientCompat.highlightCode());
    }

    private static StringBuilder ensureOutput(StringBuilder out, String text, int upTo) {
        if (out == null) {
            out = new StringBuilder(text.length() + 24);
            out.append(text, 0, upTo);
        }
        return out;
    }

    /**
     * Re-applies the colour a closing span tag restores. Vanilla colour codes and {@code §r}
     * clear the style flags in Angelica's parser, so the active styles are replayed after them;
     * {@code §x} keeps styles intact and needs no replay.
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
     * A gradient written as two inline hex colours, under either spelling.
     *
     * <p>Both markers are taken as {@code &} or {@code §} independently, because the
     * string changes spelling on its way through. What is typed is {@code &g&#..&#..},
     * and the chat converter turns every ampersand into a section sign as it is sent -
     * so the very same gradient arrives at the history above as {@code §g§#..§#..}.
     * Matching only the ampersand form meant a gradient previewed correctly while it
     * was being written and then, once submitted, fell through to plain rainbow with
     * two colours applied after it, leaving the whole line the second colour.</p>
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
