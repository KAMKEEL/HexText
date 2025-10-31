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
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FontRendererRenderPipeline {

    private final FontRendererBridge bridge;
    private final ColorStateTracker colorState = new ColorStateTracker();
    private final TextEffectController effects = new TextEffectController();
    private final List<TokenHighlight> pendingHighlights = new ArrayList<>();

    private static final int DEFAULT_OUTLINE_FALLBACK = 0x2E2E2E;

    private RenderTextData renderData;
    private boolean shadowPass;
    private boolean renderingShadow;
    private int rawTokenSkip;
    private int visibleGlyphIndex;
    private int pendingRenderColor;
    private boolean hasPendingRenderColor;
    private int outlineGlyphIndex = -1;
    private boolean outlineDrawnForCurrentGlyph;
    private ColorCodeUtils.FormattingEnvironment formattingEnvironmentIgnoreRaw;

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
        formattingEnvironmentIgnoreRaw = ColorCodeUtils.captureFormattingEnvironment(false);

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
        outlineGlyphIndex = -1;
        outlineDrawnForCurrentGlyph = false;
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
                ColorCodeUtils.FormattingEnvironment env = formattingEnvironmentIgnoreRaw;
                if (env == null) {
                    env = ColorCodeUtils.captureFormattingEnvironment(false);
                    formattingEnvironmentIgnoreRaw = env;
                }
                int tokenLength = ColorCodeUtils.detectColorCodeLengthIgnoringRaw(text, index, env);
                if (tokenLength == 0) {
                    tokenLength = detectRawAmpersandTokenLength(text, index);
                }
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

    private static int detectRawAmpersandTokenLength(CharSequence text, int index) {
        if (text == null || index < 0 || index >= text.length() - 1) {
            return 0;
        }
        if (text.charAt(index) != '&') {
            return 0;
        }
        if (text.charAt(index + 1) == '#') {
            return index + 8 <= text.length() && ColorCodeUtils.isValidHexString(text, index + 2) ? 8 : 0;
        }
        return ColorCodeUtils.isFormattingCode(text.charAt(index + 1)) ? 2 : 0;
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
        updateOutlineTracking();
        int baseColor = bridge.getTextColor();
        if (effects.hasActiveEffects()) {
            int targetColor = effects.computeColor(visibleGlyphIndex);
            int appliedColor = shadowPass ? ColorCodeUtils.calculateShadowColor(targetColor) : targetColor;
            setColorFromInt(appliedColor);
            effects.beforeGlyph(bridge.getFontRenderer(), glyph, visibleGlyphIndex, bridge.getPosX(), bridge.getPosY(),
                bridge.getFontHeight());
            float width = renderGlyphWithColor(appliedColor, italic, unicode, defaultIndex, unicodeChar, glyphRenderer);
            effects.afterGlyph();
            return width;
        }
        return renderGlyphWithColor(baseColor, italic, unicode, defaultIndex, unicodeChar, glyphRenderer);
    }

    public void advanceGlyphIndex() {
        visibleGlyphIndex++;
    }

    private float renderGlyphWithColor(int color, boolean italic, boolean unicode, int defaultIndex, char unicodeChar,
                                       GlyphRenderer glyphRenderer) {
        if (!renderingShadow && GlowingTextRenderer.isOutlineEnabled()) {
            if (!outlineDrawnForCurrentGlyph) {
                drawGlyphOutline(color, italic, unicode, defaultIndex, unicodeChar, glyphRenderer);
                outlineDrawnForCurrentGlyph = true;
            }
            applyTemporaryColor(color);
            return renderGlyphInternal(unicode, defaultIndex, unicodeChar, italic, glyphRenderer);
        }
        return renderGlyphInternal(unicode, defaultIndex, unicodeChar, italic, glyphRenderer);
    }

    private void drawGlyphOutline(int baseColor, boolean italic, boolean unicode, int defaultIndex,
                                  char unicodeChar, GlyphRenderer glyphRenderer) {
        int outlineColor = resolveOutlineColor(baseColor);
        applyTemporaryColor(outlineColor);
        for (float[] offset : GlowingTextRenderer.getOutlineOffsets()) {
            GL11.glPushMatrix();
            GL11.glTranslatef(offset[0], offset[1], 0.0f);
            renderGlyphInternal(unicode, defaultIndex, unicodeChar, italic, glyphRenderer);
            GL11.glPopMatrix();
        }
    }

    private float renderGlyphInternal(boolean unicode, int defaultIndex, char unicodeChar, boolean italic,
                                      GlyphRenderer glyphRenderer) {
        return unicode
            ? glyphRenderer.renderUnicode(unicodeChar, italic)
            : glyphRenderer.renderDefault(defaultIndex, italic);
    }

    private void applyTemporaryColor(int rgb) {
        float red = (float) ((rgb >> 16) & 255) / 255.0f;
        float green = (float) ((rgb >> 8) & 255) / 255.0f;
        float blue = (float) (rgb & 255) / 255.0f;
        bridge.applyColorComponents(red, green, blue, bridge.getAlpha());
    }

    private int resolveOutlineColor(int baseColor) {
        int outlineColor = GlowingTextRenderer.computeOutlineColor(baseColor);
        if ((outlineColor & 0xFFFFFF) == 0) {
            return DEFAULT_OUTLINE_FALLBACK;
        }
        return outlineColor;
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

    private void updateOutlineTracking() {
        if (visibleGlyphIndex != outlineGlyphIndex) {
            outlineGlyphIndex = visibleGlyphIndex;
            outlineDrawnForCurrentGlyph = false;
        }
    }

    private int resolveInitialColor() {
        if (hasPendingRenderColor) {
            hasPendingRenderColor = false;
            return pendingRenderColor & 0xFFFFFF;
        }
        int r = Math.round(bridge.getRedComponent() * 255.0f);
        int g = Math.round(bridge.getGreenComponent() * 255.0f);
        int b = Math.round(bridge.getBlueComponent() * 255.0f);
        return (r << 16) | (g << 8) | b;
    }
}
