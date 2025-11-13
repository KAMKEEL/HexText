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
                    if (ColorCodeUtils.isEffectCode(lower)) {
                        directives = ensureDirectiveMap(directives);
                        List<RenderDirective> bucket =
                            directives.computeIfAbsent(directiveIndex, key -> new ArrayList<>());
                        switch (lower) {
                            case 'g':
                                bucket.add(RenderDirectiveImpl.setRainbow(true, directiveIndex));
                                break;
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
                        if (rawMode || usingSectionSign) {
                            sanitized.append(current).append(next);
                        } else {
                            modified = true;
                        }
                        i++;
                        continue;
                    }

                    boolean isReset = ColorCodeUtils.isResetCode(lower);
                    boolean isColor = ColorCodeUtils.isMinecraftColorCode(lower);
                    boolean isStyle = ColorCodeUtils.isStyleCode(lower);

                    directives = ensureDirectiveMap(directives);
                    int bucketIndex = directiveIndex;
                    if (isStyle && !rawMode) {
                        bucketIndex = directiveIndex + 1;
                    }
                    List<RenderDirective> bucket =
                        directives.computeIfAbsent(bucketIndex, key -> new ArrayList<>());

                    if (isColor) {
                        int colorIndex = ColorCodeUtils.getMinecraftColorIndex(lower);
                        if (colorIndex >= 0) {
                            bucket.add(RenderDirectiveImpl.applyVanillaColor(colorIndex));
                        }
                    } else if (isReset) {
                        bucket.add(RenderDirectiveImpl.resetToBase());
                    } else if (isStyle) {
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

                    if (rawMode) {
                        sanitized.append(current).append(next);
                        i++;
                        continue;
                    }

                    if (usingSectionSign) {
                        sanitized.append(current);
                    } else {
                        sanitized.append('§');
                        modified = true;
                    }
                    continue;
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

    private static Map<Integer, List<RenderDirective>> normalizeDirectives(
        Map<Integer, List<RenderDirective>> directives) {
        if (directives == null || directives.isEmpty()) {
            return null;
        }
        return directives;
    }
}
