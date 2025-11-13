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
        if (lower == 'g') {
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
                bucket.add(RenderDirectiveImpl.setRainbow(true, directiveIndex));
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
