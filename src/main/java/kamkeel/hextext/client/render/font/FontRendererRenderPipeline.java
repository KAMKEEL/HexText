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
import kamkeel.hextext.common.util.ColorMath;
import kamkeel.hextext.common.util.StringUtils;
import kamkeel.hextext.config.HexTextConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FontRendererRenderPipeline {

    private static final float OUTLINE_OFFSET = 0.75f;

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
        if (effects.hasActiveEffects()) {
            int targetColor = effects.computeColor(visibleGlyphIndex);
            int appliedColor = shadowPass ? ColorCodeUtils.calculateShadowColor(targetColor) : targetColor;
            setColorFromInt(appliedColor);
            effects.beforeGlyph(bridge.getFontRenderer(), glyph, visibleGlyphIndex, bridge.getPosX(), bridge.getPosY(),
                bridge.getFontHeight());
            float width = renderGlyphWithOutline(glyph, italic, unicode, defaultIndex, unicodeChar, glyphRenderer,
                appliedColor);
            effects.afterGlyph();
            return width;
        }

        return renderGlyphWithOutline(glyph, italic, unicode, defaultIndex, unicodeChar, glyphRenderer,
            bridge.getTextColor());
    }

    private float renderGlyphWithOutline(char glyph, boolean italic, boolean unicode, int defaultIndex, char unicodeChar,
            GlyphRenderer glyphRenderer, int glyphColor) {
        if (shouldRenderOutline(glyphColor)) {
            renderOutline(glyph, italic, unicode, defaultIndex, unicodeChar, glyphRenderer, glyphColor);
        }
        return drawGlyph(glyph, italic, unicode, defaultIndex, unicodeChar, glyphRenderer);
    }

    private void renderOutline(char glyph, boolean italic, boolean unicode, int defaultIndex, char unicodeChar,
            GlyphRenderer glyphRenderer, int glyphColor) {
        int outlineColor = computeOutlineColor(glyphColor);
        if ((outlineColor & 0xFFFFFF) == 0) {
            return;
        }

        float originX = bridge.getPosX();
        float originY = bridge.getPosY();
        float alpha = bridge.getAlpha();
        float red = (float) ((outlineColor >> 16) & 255) / 255.0f;
        float green = (float) ((outlineColor >> 8) & 255) / 255.0f;
        float blue = (float) (outlineColor & 255) / 255.0f;

        bridge.applyColorComponents(red, green, blue, alpha);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                bridge.setPos(originX + dx * OUTLINE_OFFSET, originY + dy * OUTLINE_OFFSET);
                drawGlyph(glyph, italic, unicode, defaultIndex, unicodeChar, glyphRenderer);
            }
        }

        bridge.setPos(originX, originY);
        applyColorComponents(glyphColor);
    }

    private float drawGlyph(char glyph, boolean italic, boolean unicode, int defaultIndex, char unicodeChar,
            GlyphRenderer glyphRenderer) {
        return unicode
            ? glyphRenderer.renderUnicode(unicodeChar, italic)
            : glyphRenderer.renderDefault(defaultIndex, italic);
    }

    private boolean shouldRenderOutline(int glyphColor) {
        return HexTextConfig.isTextOutlineEnabled() && !renderingShadow && (glyphColor & 0xFFFFFF) != 0;
    }

    private int computeOutlineColor(int glyphColor) {
        int base = glyphColor & 0xFFFFFF;
        int brightened = ColorMath.scaleBrightness(base, 1.35f);
        int combined = ColorMath.blend(brightened, base, 0.35f);
        return ColorMath.blend(combined, 0xFFFFFF, 0.25f);
    }

    private void applyColorComponents(int rgb) {
        int masked = rgb & 0xFFFFFF;
        float red = (float) ((masked >> 16) & 255) / 255.0f;
        float green = (float) ((masked >> 8) & 255) / 255.0f;
        float blue = (float) (masked & 255) / 255.0f;
        bridge.applyColorComponents(red, green, blue, bridge.getAlpha());
    }

    public void advanceGlyphIndex() {
        visibleGlyphIndex++;
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
