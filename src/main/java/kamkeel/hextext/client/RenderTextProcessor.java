package kamkeel.hextext.client;

import kamkeel.hextext.util.ColorCodeUtils;
import kamkeel.hextext.util.StringUtils;

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

        for (int i = 0; i < processed.length(); i++) {
            char current = processed.charAt(i);

            if (current == '&' && i + 1 < processed.length()) {
                char next = processed.charAt(i + 1);
                int instructionIndex = sanitized.length();

                if (ColorCodeUtils.isValidHexString(processed, i + 1)) {
                    int rgb = ColorCodeUtils.parseHexColor(processed, i + 1);
                    if (rgb != -1) {
                        instructions = ensureInstructionMap(instructions);
                        instructions.computeIfAbsent(instructionIndex, key -> new ArrayList<>())
                            .add(RenderInstruction.apply(rgb, true));
                        if (rawMode) {
                            sanitized.append('&');
                            sanitized.append(processed, i + 1, i + 7);
                        }
                        modified = true;
                        i += 6;
                        continue;
                    }
                }

                if (ColorCodeUtils.isFormattingCode(next)) {
                    boolean isReset = ColorCodeUtils.isResetCode(next);
                    if (isReset) {
                        instructions = ensureInstructionMap(instructions);
                        instructions.computeIfAbsent(instructionIndex, key -> new ArrayList<>())
                            .add(RenderInstruction.resetToBase());
                    }
                    if (rawMode) {
                        sanitized.append('§').append(next);
                        sanitized.append('&').append(next);
                        modified = true;
                        i++;
                        continue;
                    } else {
                        sanitized.append('§');
                        modified = true;
                        continue;
                    }
                }
            }

            if (current == '<') {
                if (i + 8 <= processed.length() && processed.charAt(i + 7) == '>'
                    && ColorCodeUtils.isValidHexString(processed, i + 1)) {
                    int rgb = ColorCodeUtils.parseHexColor(processed, i + 1);
                    if (rgb != -1) {
                        instructions = ensureInstructionMap(instructions);
                        instructions.computeIfAbsent(sanitized.length(), key -> new ArrayList<>())
                            .add(RenderInstruction.push(rgb));
                        if (rawMode) {
                            sanitized.append(processed, i, i + 8);
                        }
                        modified = true;
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
                    }
                    modified = true;
                    i += 8;
                    continue;
                }
            }

            sanitized.append(current);
        }

        if (!modified && (instructions == null || instructions.isEmpty())) {
            return RenderTextData.unchanged();
        }

        return RenderTextData.withDisplayText(sanitized.toString(), normalizeInstructions(instructions));
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
