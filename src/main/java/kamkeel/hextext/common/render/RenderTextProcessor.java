package kamkeel.hextext.common.render;

import kamkeel.hextext.HexText;
import kamkeel.hextext.api.rendering.RenderDirective;
import kamkeel.hextext.api.rendering.RenderPlan;
import kamkeel.hextext.common.util.ColorCodeUtils;
import kamkeel.hextext.common.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prepares the text and associated render directives for the font renderer mixin.
 */
public final class RenderTextProcessor {

    private RenderTextProcessor() {
    }

    public static RenderPlan prepare(String text, boolean rawMode) {
        if (text == null || text.isEmpty()) {
            return RenderPlanImpl.unchanged();
        }

        String processed = rawMode ? StringUtils.normalizeForRawDisplay(text) : text;
        StringBuilder sanitized = new StringBuilder(processed.length() + (rawMode ? 16 : 0));
        Map<Integer, List<RenderDirective>> directives = null;
        boolean modified = rawMode && !processed.equals(text);

        final boolean allowHtml = HexText.getActiveProxy() == null || HexText.getActiveProxy().allowHtmlFormatting();
        final boolean allowAmpersand = rawMode || HexText.getActiveProxy() == null
            || HexText.getActiveProxy().allowUniversalAmpersand();

        for (int i = 0; i < processed.length(); i++) {
            char current = processed.charAt(i);

            // An escaped marker is text. Rendered, the backslash is dropped and the code
            // shows as its own characters; in an editor it stays, because the reader is
            // looking at what they typed and the width walkers count it.
            if (current == '\\' && i + 1 < processed.length()) {
                char escaped = processed.charAt(i + 1);
                if (escaped == '&' || escaped == 167) {
                    if (rawMode) {
                        sanitized.append(current).append(escaped);
                    } else {
                        sanitized.append(escaped);
                        modified = true;
                    }
                    i++;
                    continue;
                }
            }

            boolean usingSectionSign = current == 167;
            boolean usingAmpersand = current == '&';

            if ((usingSectionSign || (usingAmpersand && allowAmpersand)) && i + 1 < processed.length()) {
                int directiveIndex = sanitized.length();

                if (processed.charAt(i + 1) == '#') {
                    int hexStart = i + 2;
                    if (hexStart + 6 <= processed.length() && ColorCodeUtils.isValidHexString(processed, hexStart)) {
                        int rgb = ColorCodeUtils.parseHexColor(processed, hexStart);
                        if (rgb != -1) {
                            directives = ensureDirectiveMap(directives);
                            directives.computeIfAbsent(directiveIndex, key -> new ArrayList<>())
                                .add(RenderDirectiveImpl.apply(rgb, true));
                            if (rawMode) {
                                sanitized.append(current).append('#');
                                sanitized.append(processed, hexStart, hexStart + 6);
                            } else {
                                modified = true;
                            }
                            i += 7;
                            continue;
                        }
                    }
                }

                // A shadow tint, which is a code carrying a colour rather than a flag.
                // Written bare it clears the tint again, so the darkened base colour
                // comes back without needing a full reset.
                if (Character.toLowerCase(processed.charAt(i + 1)) == 'u') {
                    int shadowRgb = -1;
                    int after = i + 2;
                    if (after + 8 <= processed.length() && isHexMarker(processed, after)
                        && ColorCodeUtils.isValidHexString(processed, after + 2)) {
                        shadowRgb = ColorCodeUtils.parseHexColor(processed, after + 2);
                    }
                    directives = ensureDirectiveMap(directives);
                    directives.computeIfAbsent(directiveIndex, key -> new ArrayList<>())
                        .add(RenderDirectiveImpl.setShadowColor(Math.max(shadowRgb, 0), shadowRgb != -1));
                    int consumed = shadowRgb != -1 ? 10 : 2;
                    if (rawMode) {
                        sanitized.append(processed, i, i + consumed);
                    } else {
                        modified = true;
                    }
                    i += consumed - 1;
                    continue;
                }

                // Checked ahead of the plain 'g', which is rainbow. A gradient is the
                // same letter carrying two colours, and reading it as rainbow left the
                // two colours to apply in turn with the second one winning - a flat
                // line in the end colour, which is what it looked like.
                if (Character.toLowerCase(processed.charAt(i + 1)) == 'g') {
                    int gradientEnd = gradientTokenEnd(processed, i);
                    if (gradientEnd > 0) {
                        int startRgb = ColorCodeUtils.parseHexColor(processed, i + 4);
                        int endRgb = ColorCodeUtils.parseHexColor(processed, i + 12);
                        if (startRgb != -1 && endRgb != -1) {
                            directives = ensureDirectiveMap(directives);
                            final int span = countVisibleGlyphs(processed, gradientEnd);
                            if (rawMode) {
                                // The token names two colours, so it wears them - and the
                                // ramp starts after it rather than spending itself on the
                                // code's own characters.
                                addDirective(directives, directiveIndex, RenderDirectiveImpl.apply(startRgb, true));
                                addDirective(directives, directiveIndex + SECOND_COLOR_OFFSET,
                                    RenderDirectiveImpl.apply(endRgb, true));
                                sanitized.append(processed, i, gradientEnd);
                                addDirective(directives, sanitized.length(),
                                    RenderDirectiveImpl.setGradient(startRgb, endRgb, span));
                            } else {
                                addDirective(directives, directiveIndex,
                                    RenderDirectiveImpl.setGradient(startRgb, endRgb, span));
                                modified = true;
                            }
                            i = gradientEnd - 1;
                            continue;
                        }
                    }
                }

                // The other spelling of an inline hex colour, and the same directive:
                // a colour that also clears the styles standing in front of it.
                if (usingSectionSign) {
                    int sectionX = ColorCodeUtils.parseSectionX(processed, i);
                    if (sectionX != -1) {
                        directives = ensureDirectiveMap(directives);
                        directives.computeIfAbsent(directiveIndex, key -> new ArrayList<>())
                            .add(RenderDirectiveImpl.apply(sectionX, true));
                        if (rawMode) {
                            sanitized.append(processed, i, i + ColorCodeUtils.SECTION_X_LENGTH);
                        } else {
                            modified = true;
                        }
                        i += ColorCodeUtils.SECTION_X_LENGTH - 1;
                        continue;
                    }
                }

                char next = processed.charAt(i + 1);
                char lower = Character.toLowerCase(next);

                if (ColorCodeUtils.isFormattingCode(lower)) {
                    FormatCategory category = classifyFormatting(lower);
                    if (category != null) {
                        directives = ensureDirectiveMap(directives);
                        List<RenderDirective> bucket =
                            directives.computeIfAbsent(directiveIndex, key -> new ArrayList<>());
                        emitFormattingDirective(bucket, category, lower, directiveIndex);

                        boolean usingLiteral = rawMode || usingSectionSign;
                        if (usingLiteral) {
                            sanitized.append(current);
                            if (category.consumesTrailingCode()) {
                                sanitized.append(next);
                                i++;
                            }
                            continue;
                        }

                        if (category.keepWhenUsingAmpersand()) {
                            sanitized.append('§');
                            modified = true;
                            continue;
                        }

                        modified = true;
                        if (category.consumesTrailingCode()) {
                            i++;
                        }
                        continue;
                    }
                }
            }

            if (allowHtml && current == '<') {
                if (i + 8 <= processed.length() && processed.charAt(i + 7) == '>'
                    && ColorCodeUtils.isValidHexString(processed, i + 1)) {
                    int rgb = ColorCodeUtils.parseHexColor(processed, i + 1);
                    if (rgb != -1) {
                        directives = ensureDirectiveMap(directives);
                        directives.computeIfAbsent(sanitized.length(), key -> new ArrayList<>())
                            .add(RenderDirectiveImpl.push(rgb));
                        if (rawMode) {
                            sanitized.append(processed, i, i + 8);
                        } else {
                            modified = true;
                        }
                        i += 7;
                        continue;
                    }
                }

                if (i + 9 <= processed.length() && processed.charAt(i + 1) == '/'
                    && processed.charAt(i + 8) == '>' && ColorCodeUtils.isValidHexString(processed, i + 2)) {
                    directives = ensureDirectiveMap(directives);
                    directives.computeIfAbsent(sanitized.length(), key -> new ArrayList<>())
                        .add(RenderDirectiveImpl.pop());
                    if (rawMode) {
                        sanitized.append(processed, i, i + 9);
                    } else {
                        modified = true;
                    }
                    i += 8;
                    continue;
                }
            }

            sanitized.append(current);
        }

        Map<Integer, List<RenderDirective>> normalized = normalizeDirectives(directives);

        if (!modified && (normalized == null || normalized.isEmpty())) {
            return RenderPlanImpl.unchanged();
        }

        if (!modified && normalized != null) {
            return RenderPlanImpl.withInstructions(normalized);
        }

        return RenderPlanImpl.withDisplayText(sanitized.toString(), normalized);
    }

    private static Map<Integer, List<RenderDirective>> ensureDirectiveMap(
        Map<Integer, List<RenderDirective>> directives) {
        return directives != null ? directives : new HashMap<>();
    }

    /**
     * Where {@code [&§]g[&§]#RRGGBB[&§]#RRGGBB} ends, or {@code -1}. Each marker is taken
     * as either spelling: the send conversion rewrites ampersands on the way out.
     */
    /** Where a gradient token's second colour begins, from the token's own start. */
    private static final int SECOND_COLOR_OFFSET = 10;

    private static void addDirective(Map<Integer, List<RenderDirective>> directives, int index,
        RenderDirective directive) {
        directives.computeIfAbsent(index, key -> new ArrayList<>(2)).add(directive);
    }

    private static int gradientTokenEnd(CharSequence text, int start) {
        if (start + 18 > text.length()) {
            return -1;
        }
        if (!isHexMarker(text, start + 2) || !ColorCodeUtils.isValidHexString(text, start + 4)) {
            return -1;
        }
        if (!isHexMarker(text, start + 10) || !ColorCodeUtils.isValidHexString(text, start + 12)) {
            return -1;
        }
        return start + 18;
    }

    private static boolean isHexMarker(CharSequence text, int at) {
        char marker = text.charAt(at);
        return (marker == '&' || marker == 167) && text.charAt(at + 1) == '#';
    }

    /**
     * How many glyphs a gradient has to travel across. Counted up front because the ramp
     * needs its length before the first glyph. Codes are zero-width and skipped, and
     * counting stops at the first code carrying a colour, where the ramp ends.
     */
    private static int countVisibleGlyphs(CharSequence text, int from) {
        int visible = 0;
        for (int i = from; i < text.length(); ) {
            int code = ColorCodeUtils.detectColorCodeLength(text, i);
            if (code > 0) {
                if (carriesColor(text, i, code)) {
                    return visible;
                }
                i += code;
                continue;
            }
            visible++;
            i++;
        }
        return visible;
    }

    /** Whether this code sets a colour, which is where a running ramp ends. */
    private static boolean carriesColor(CharSequence text, int at, int codeLength) {
        if (codeLength != 2) {
            // Hex under either spelling, §x forms, and both span tags.
            return true;
        }
        char code = Character.toLowerCase(text.charAt(at + 1));
        return ColorCodeUtils.isMinecraftColorCode(code) || ColorCodeUtils.isResetCode(code)
            || code == 'g';
    }

    private static FormatCategory classifyFormatting(char lower) {
        if (ColorCodeUtils.isMinecraftColorCode(lower)) {
            return FormatCategory.COLOR;
        }
        if (ColorCodeUtils.isResetCode(lower)) {
            return FormatCategory.RESET;
        }
        if (ColorCodeUtils.isStyleCode(lower)) {
            return FormatCategory.STYLE;
        }
        if (lower == 'g' || lower == 'q') {
            return FormatCategory.RAINBOW;
        }
        if (ColorCodeUtils.isEffectCode(lower)) {
            return FormatCategory.EFFECT;
        }
        return null;
    }

    private static void emitFormattingDirective(List<RenderDirective> bucket, FormatCategory category, char lower,
        int directiveIndex) {
        switch (category) {
            case COLOR:
                int colorIndex = ColorCodeUtils.getMinecraftColorIndex(lower);
                if (colorIndex >= 0) {
                    bucket.add(RenderDirectiveImpl.applyVanillaColor(colorIndex));
                }
                break;
            case RESET:
                bucket.add(RenderDirectiveImpl.resetToBase());
                break;
            case STYLE:
                applyStyleDirective(bucket, lower);
                break;
            case EFFECT:
                applyEffectDirective(bucket, lower);
                break;
            case RAINBOW:
                // g cycles, q holds still - Angelica's q is a fixed table and the two
                // renderers have to agree about the same string.
                bucket.add(lower == 'q'
                    ? RenderDirectiveImpl.setStaticRainbow(directiveIndex)
                    : RenderDirectiveImpl.setRainbow(true, directiveIndex));
                break;
            default:
                break;
        }
    }

    private static void applyStyleDirective(List<RenderDirective> bucket, char lower) {
        switch (lower) {
            case 'k':
                bucket.add(RenderDirectiveImpl.setRandom(true));
                break;
            case 'l':
                bucket.add(RenderDirectiveImpl.setBold(true));
                break;
            case 'm':
                bucket.add(RenderDirectiveImpl.setStrikethrough(true));
                break;
            case 'n':
                bucket.add(RenderDirectiveImpl.setUnderline(true));
                break;
            case 'o':
                bucket.add(RenderDirectiveImpl.setItalic(true));
                break;
            default:
                break;
        }
    }

    private static void applyEffectDirective(List<RenderDirective> bucket, char lower) {
        switch (lower) {
            case 'h':
                bucket.add(RenderDirectiveImpl.setDinnerbone(true));
                break;
            case 'i':
                bucket.add(RenderDirectiveImpl.setIgnite(true));
                break;
            case 'z':
                bucket.add(RenderDirectiveImpl.setWave(true));
                break;
            case 'j':
                bucket.add(RenderDirectiveImpl.setShake(true));
                break;
            default:
                break;
        }
    }

    private static Map<Integer, List<RenderDirective>> normalizeDirectives(
        Map<Integer, List<RenderDirective>> directives) {
        if (directives == null || directives.isEmpty()) {
            return null;
        }
        return directives;
    }

    private enum FormatCategory {
        COLOR(true, false),
        RESET(true, false),
        STYLE(true, false),
        EFFECT(false, true),
        RAINBOW(false, true);

        private final boolean keepWhenAmpersand;
        private final boolean consumesTrailingCode;

        FormatCategory(boolean keepWhenAmpersand, boolean consumesTrailingCode) {
            this.keepWhenAmpersand = keepWhenAmpersand;
            this.consumesTrailingCode = consumesTrailingCode;
        }

        boolean keepWhenUsingAmpersand() {
            return keepWhenAmpersand;
        }

        boolean consumesTrailingCode() {
            return consumesTrailingCode;
        }
    }
}
