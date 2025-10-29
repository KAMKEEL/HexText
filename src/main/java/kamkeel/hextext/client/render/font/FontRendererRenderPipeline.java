package kamkeel.hextext.client.render.font;

import kamkeel.hextext.client.render.ColorStateTracker;
import kamkeel.hextext.client.render.FontRenderContext;
import kamkeel.hextext.client.render.FontRendererUtils;
import kamkeel.hextext.client.render.RenderInstruction;
import kamkeel.hextext.client.render.RenderTextData;
import kamkeel.hextext.client.render.RenderTextProcessor;
import kamkeel.hextext.client.render.TextEffectController;
import kamkeel.hextext.client.render.TokenHighlight;
import kamkeel.hextext.client.render.TokenHighlightUtils;
import kamkeel.hextext.common.util.ColorCodeUtils;
import kamkeel.hextext.common.util.StringUtils;
import kamkeel.hextext.config.HexTextConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FontRendererRenderPipeline {

    private final FontRendererBridge bridge;
    private final ColorStateTracker colorState = new ColorStateTracker();
    private final TextEffectController effects = new TextEffectController();
    private final List<TokenHighlight> pendingHighlights = new ArrayList<>();

    private RenderTextData renderData;
    private boolean shadowPass;
    private boolean renderingShadow;
    private int rawTokenSkip;
    private int visibleGlyphIndex;
    private int pendingRenderColor;
    private boolean hasPendingRenderColor;

    public FontRendererRenderPipeline(FontRendererBridge bridge) {
        this.bridge = bridge;
    }

    public void capturePreparedColor(int color) {
        pendingRenderColor = color & 0xFFFFFF;
        hasPendingRenderColor = true;
    }

    public void begin(String text, boolean shadow) {
        boolean rawMode = FontRenderContext.isRawTextRendering();
        renderData = RenderTextProcessor.prepare(text, rawMode);
        shadowPass = shadow;
        renderingShadow = shadow;

        int initialColor = resolveInitialColor();
        colorState.begin(initialColor, shadow);
        bridge.setTextColor(initialColor);
        effects.begin(initialColor);

        if (rawMode) {
            bridge.resetFormattingStyles();
        }

        if (!shadow && rawMode) {
            pendingHighlights.clear();
        } else {
            pendingHighlights.clear();
        }

        rawTokenSkip = 0;
        visibleGlyphIndex = 0;
    }

    public String adjustRenderText(String text) {
        if (renderData != null && renderData.shouldReplaceText()) {
            return renderData.getDisplayText();
        }
        return text;
    }

    public void applyInstructions(String text, int index, char currentChar) {
        if (!renderingShadow && FontRenderContext.isRawTextRendering()) {
            if (rawTokenSkip > 0) {
                rawTokenSkip--;
            } else {
                int tokenLength = ColorCodeUtils.detectColorCodeLengthIgnoringRaw(text, index);
                if (tokenLength > 0) {
                    if (text.charAt(index) != 167) {
                        float width = TokenHighlightUtils.measureLiteralWidth(bridge.getFontRenderer(), text, index, tokenLength);
                        if (width > 0.0f) {
                            pendingHighlights.add(new TokenHighlight(bridge.getPosX(), bridge.getPosY(), width,
                                TokenHighlightUtils.getTokenHighlightColor(text, index)));
                        }
                        rawTokenSkip = Math.max(tokenLength - 1, 0);
                    }
                }
            }
        }

        if (renderData == null || !renderData.hasInstructions()) {
            return;
        }

        Map<Integer, List<RenderInstruction>> instructionsByIndex = renderData.getInstructions();
        if (instructionsByIndex == null) {
            return;
        }

        List<RenderInstruction> instructions = instructionsByIndex.remove(index);
        if (instructions == null) {
            return;
        }

        for (RenderInstruction instruction : instructions) {
            executeInstruction(instruction);
        }
    }

    public void end() {
        renderData = null;
        if (!renderingShadow && !pendingHighlights.isEmpty()) {
            TokenHighlightUtils.drawHighlights(pendingHighlights, bridge.getFontHeight());
            pendingHighlights.clear();
        }
    }

    public int computeStringWidth(String text) {
        boolean rawMode = FontRenderContext.isRawTextRendering();
        return Math.round(FontRendererUtils.calculateMaxLineWidth(bridge.getFontRenderer(), text, rawMode));
    }

    public int computeLineBreakIndex(String text, int maxWidth) {
        boolean rawMode = FontRenderContext.isRawTextRendering();
        return FontRendererUtils.computeLineBreakIndex(bridge.getFontRenderer(), text, maxWidth, rawMode);
    }

    public String trimStringToWidth(String text, int width, boolean reverse) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        boolean rawMode = FontRenderContext.isRawTextRendering();
        if (!reverse) {
            int endIndex = FontRendererUtils.computeLineBreakIndex(bridge.getFontRenderer(), text, width, rawMode);
            return text.substring(0, Math.min(endIndex, text.length()));
        }
        return FontRendererUtils.trimStringFromEnd(bridge.getFontRenderer(), text, width, rawMode);
    }

    public String wrapFormattedString(String text, int width) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        boolean rawMode = FontRenderContext.isRawTextRendering();
        return FontRendererUtils.wrapFormattedString(bridge.getFontRenderer(), text, width, rawMode);
    }

    public String extractFormatFromString(String text) {
        return StringUtils.extractFormatFromString(text);
    }

    public float renderGlyph(char glyph, boolean italic, boolean unicode, int defaultIndex, char unicodeChar,
            GlyphRenderer glyphRenderer) {
        float width;
        if (effects.hasActiveEffects()) {
            int targetColor = effects.computeColor(visibleGlyphIndex);
            int appliedColor = shadowPass ? ColorCodeUtils.calculateShadowColor(targetColor) : targetColor;
            renderOutlineForGlyph(glyph, italic, unicode, defaultIndex, unicodeChar, glyphRenderer, appliedColor, true);
            setColorFromInt(appliedColor);
            effects.beforeGlyph(bridge.getFontRenderer(), glyph, visibleGlyphIndex, bridge.getPosX(), bridge.getPosY(),
                bridge.getFontHeight());
            width = unicode
                ? glyphRenderer.renderUnicode(unicodeChar, italic)
                : glyphRenderer.renderDefault(defaultIndex, italic);
            effects.afterGlyph();
        } else {
            int currentColor = bridge.getTextColor();
            renderOutlineForGlyph(glyph, italic, unicode, defaultIndex, unicodeChar, glyphRenderer, currentColor, false);
            width = unicode
                ? glyphRenderer.renderUnicode(unicodeChar, italic)
                : glyphRenderer.renderDefault(defaultIndex, italic);
        }
        return width;
    }

    public void advanceGlyphIndex() {
        visibleGlyphIndex++;
    }

    private void renderOutlineForGlyph(char glyph, boolean italic, boolean unicode, int defaultIndex, char unicodeChar,
            GlyphRenderer glyphRenderer, int baseColor, boolean applyEffects) {
        if (!shouldRenderOutline() || glyph == 0 || glyph == ' ') {
            return;
        }

        int maskedBase = baseColor & 0xFFFFFF;
        int outlineColor = ColorCodeUtils.calculateOutlineColor(maskedBase);
        float baseX = bridge.getPosX();
        float baseY = bridge.getPosY();
        float alpha = bridge.getAlpha();
        float baseRed = extractColorComponent(maskedBase, 16);
        float baseGreen = extractColorComponent(maskedBase, 8);
        float baseBlue = extractColorComponent(maskedBase, 0);
        float outlineRed = extractColorComponent(outlineColor, 16);
        float outlineGreen = extractColorComponent(outlineColor, 8);
        float outlineBlue = extractColorComponent(outlineColor, 0);

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                if (offsetX == 0 && offsetY == 0) {
                    continue;
                }
                bridge.setPos(baseX + offsetX, baseY + offsetY);
                bridge.applyColorComponents(outlineRed, outlineGreen, outlineBlue, alpha);
                if (applyEffects) {
                    effects.beforeGlyph(bridge.getFontRenderer(), glyph, visibleGlyphIndex, bridge.getPosX(), bridge.getPosY(),
                        bridge.getFontHeight());
                }
                if (unicode) {
                    glyphRenderer.renderUnicode(unicodeChar, italic);
                } else {
                    glyphRenderer.renderDefault(defaultIndex, italic);
                }
                if (applyEffects) {
                    effects.afterGlyph();
                }
            }
        }

        bridge.setPos(baseX, baseY);
        bridge.applyColorComponents(baseRed, baseGreen, baseBlue, alpha);
    }

    private boolean shouldRenderOutline() {
        return HexTextConfig.isGlowingTextOutlineEnabled() && !renderingShadow;
    }

    private static float extractColorComponent(int color, int shift) {
        return ((color >> shift) & 0xFF) / 255.0f;
    }

    private void executeInstruction(RenderInstruction instruction) {
        boolean resetStyles = instruction.resetsFormatting();

        switch (instruction.getType()) {
            case APPLY_RGB:
                int appliedRgb = colorState.applyRgb(instruction.getRgb(), instruction.shouldClearStack(), effects, renderingShadow);
                setColorFromInt(appliedRgb);
                break;
            case APPLY_VANILLA_COLOR:
                int vanillaColor = colorState.applyVanillaColor(instruction.getParameter(), bridge.getColorCodePalette(),
                    instruction.shouldClearStack(), effects, renderingShadow);
                setColorFromInt(vanillaColor);
                break;
            case PUSH_RGB:
                int pushedColor = colorState.push(instruction.getRgb(), effects, renderingShadow);
                setColorFromInt(pushedColor);
                break;
            case POP_COLOR:
                int restoredColor = colorState.pop(effects, renderingShadow);
                setColorFromInt(restoredColor);
                break;
            case RESET_TO_BASE:
                int baseColor = colorState.resetToBase(effects, renderingShadow);
                setColorFromInt(baseColor);
                break;
            case SET_RANDOM:
                bridge.setRandomStyle(instruction.isEnabled());
                break;
            case SET_BOLD:
                bridge.setBoldStyle(instruction.isEnabled());
                break;
            case SET_STRIKETHROUGH:
                bridge.setStrikethroughStyle(instruction.isEnabled());
                break;
            case SET_UNDERLINE:
                bridge.setUnderlineStyle(instruction.isEnabled());
                break;
            case SET_ITALIC:
                bridge.setItalicStyle(instruction.isEnabled());
                break;
            case SET_RAINBOW:
                if (instruction.shouldClearStack()) {
                    colorState.clearStacks();
                }
                effects.resetDynamicEffects();
                effects.setRainbow(instruction.isEnabled(), visibleGlyphIndex);
                if (!renderingShadow) {
                    effects.updateBaseColor(bridge.getTextColor());
                }
                resetStyles = true;
                break;
            case SET_DINNERBONE:
                effects.setDinnerbone(instruction.isEnabled());
                break;
            case SET_IGNITE:
                if (instruction.isEnabled() && !renderingShadow) {
                    effects.updateBaseColor(bridge.getTextColor());
                }
                effects.setIgnite(instruction.isEnabled());
                break;
            case SET_SHAKE:
                effects.setShake(instruction.isEnabled());
                break;
        }

        if (resetStyles) {
            bridge.resetFormattingStyles();
        }
    }

    private void setColorFromInt(int rgb) {
        int masked = rgb & 0xFFFFFF;
        bridge.setTextColor(masked);
        colorState.setCurrentColor(masked);
        float red = (float) (rgb >> 16 & 255) / 255.0F;
        float green = (float) (rgb >> 8 & 255) / 255.0F;
        float blue = (float) (rgb & 255) / 255.0F;
        bridge.applyColorComponents(red, green, blue, bridge.getAlpha());
    }

    private int resolveInitialColor() {
        if (hasPendingRenderColor) {
            hasPendingRenderColor = false;
            return pendingRenderColor & 0xFFFFFF;
        }
        int r = Math.round(bridge.getRedComponent() * 255.0f);
        int g = Math.round(bridge.getBlueComponent() * 255.0f);
        int b = Math.round(bridge.getGreenComponent() * 255.0f);
        return (r << 16) | (g << 8) | b;
    }
}
