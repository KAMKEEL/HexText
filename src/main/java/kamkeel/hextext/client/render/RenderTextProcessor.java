package kamkeel.hextext.client.render;

import kamkeel.hextext.HexText;
import kamkeel.hextext.common.util.ColorCodeUtils;
import kamkeel.hextext.common.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prepares the text and associated render instructions for the font renderer mixin.
 */
public final class RenderTextProcessor {

    private RenderTextProcessor() {}

    public static RenderTextData prepare(String text, boolean rawMode) {
        if (text == null || text.isEmpty()) {
            return RenderTextData.unchanged();
        }

        String processed = rawMode ? StringUtils.normalizeForRawDisplay(text) : text;
        StringBuilder sanitized = new StringBuilder(processed.length() + (rawMode ? 16 : 0));
        Map<Integer, List<RenderInstruction>> instructions = null;
        boolean modified = rawMode && !processed.equals(text);

        boolean allowAmpersand = HexText.getActiveProxy().allowAmpersand();
        boolean allowHtml = HexText.getActiveProxy().allowHtmlFormatting();

        for (int i = 0; i < processed.length(); i++) {
            char current = processed.charAt(i);

            boolean usingSectionSign = current == 167;
            boolean usingAmpersand = current == '&';

            if ((usingSectionSign || (usingAmpersand && allowAmpersand)) && i + 1 < processed.length()) {
                int instructionIndex = sanitized.length();

                if (processed.charAt(i + 1) == '#') {
                    int hexStart = i + 2;
                    if (hexStart + 6 <= processed.length() && ColorCodeUtils.isValidHexString(processed, hexStart)) {
                        int rgb = ColorCodeUtils.parseHexColor(processed, hexStart);
                        if (rgb != -1) {
                            instructions = ensureInstructionMap(instructions);
                            instructions.computeIfAbsent(instructionIndex, key -> new ArrayList<>())
                                .add(RenderInstruction.apply(rgb, true));
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
                        instructions = ensureInstructionMap(instructions);
                        List<RenderInstruction> bucket =
                            instructions.computeIfAbsent(instructionIndex, key -> new ArrayList<>());
                        switch (lower) {
                            case 'g':
                                bucket.add(RenderInstruction.setRainbow(true, instructionIndex));
                                break;
                            case 'h':
                                bucket.add(RenderInstruction.setDinnerbone(true));
                                break;
                            case 'i':
                                bucket.add(RenderInstruction.setIgnite(true));
                                break;
                            case 'j':
                                bucket.add(RenderInstruction.setShake(true));
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

                    instructions = ensureInstructionMap(instructions);
                    List<RenderInstruction> bucket =
                        instructions.computeIfAbsent(instructionIndex, key -> new ArrayList<>());

                    if (isColor) {
                        int colorIndex = ColorCodeUtils.getMinecraftColorIndex(lower);
                        if (colorIndex >= 0) {
                            bucket.add(RenderInstruction.applyVanillaColor(colorIndex));
                        }
                    } else if (isReset) {
                        bucket.add(RenderInstruction.resetToBase());
                    } else if (isStyle) {
                        switch (lower) {
                            case 'k':
                                bucket.add(RenderInstruction.setRandom(true));
                                break;
                            case 'l':
                                bucket.add(RenderInstruction.setBold(true));
                                break;
                            case 'm':
                                bucket.add(RenderInstruction.setStrikethrough(true));
                                break;
                            case 'n':
                                bucket.add(RenderInstruction.setUnderline(true));
                                break;
                            case 'o':
                                bucket.add(RenderInstruction.setItalic(true));
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
                        instructions = ensureInstructionMap(instructions);
                        instructions.computeIfAbsent(sanitized.length(), key -> new ArrayList<>())
                            .add(RenderInstruction.push(rgb));
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
                    instructions = ensureInstructionMap(instructions);
                    instructions.computeIfAbsent(sanitized.length(), key -> new ArrayList<>())
                        .add(RenderInstruction.pop());
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

        Map<Integer, List<RenderInstruction>> normalizedInstructions = normalizeInstructions(instructions);

        if (!modified && (normalizedInstructions == null || normalizedInstructions.isEmpty())) {
            return RenderTextData.unchanged();
        }

        if (!modified && normalizedInstructions != null) {
            return RenderTextData.withInstructions(normalizedInstructions);
        }

        return RenderTextData.withDisplayText(sanitized.toString(), normalizedInstructions);
    }

    private static Map<Integer, List<RenderInstruction>> ensureInstructionMap(
            Map<Integer, List<RenderInstruction>> instructions) {
        return instructions != null ? instructions : new HashMap<>();
    }

    private static Map<Integer, List<RenderInstruction>> normalizeInstructions(
            Map<Integer, List<RenderInstruction>> instructions) {
        return (instructions == null || instructions.isEmpty()) ? null : instructions;
    }
}
