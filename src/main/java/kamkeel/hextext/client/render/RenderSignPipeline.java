package kamkeel.hextext.client.render;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.client.render.font.GlowingTextRenderer;
import kamkeel.hextext.api.sign.IHexTextSign;
import kamkeel.hextext.api.sign.SignSide;
import kamkeel.hextext.common.compat.AngelicaCompatibility;
import kamkeel.hextext.common.util.ColorCodeUtils;
import kamkeel.hextext.common.util.ColorMath;
import kamkeel.hextext.common.util.StringUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.tileentity.TileEntitySign;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RenderSignPipeline {

    public static void renderTextForSide(TileEntitySign sign, IHexTextSign state, FontRenderer fontRenderer,
                                   SignSide side, float scale, float baseLightU, float baseLightV) {

        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 0.5F * scale, side == SignSide.FRONT ? 0.07F * scale : -0.07F * scale);
        if (side == SignSide.BACK) {
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
        }

        float fontScale = 0.016666668F * scale;
        GL11.glScalef(fontScale, -fontScale, fontScale);
        GL11.glNormal3f(0.0F, 0.0F, -1.0F * fontScale);

        boolean glowing = state.isGlowing(side);
        boolean outlined = state.isOutlined(side);

        GL11.glDepthMask(false);
        boolean changedLightmap = false;
        if (glowing) {
            GL11.glDisable(GL11.GL_LIGHTING);
            if (OpenGlHelper.lastBrightnessX != 240.0F || OpenGlHelper.lastBrightnessY != 240.0F) {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
                changedLightmap = true;
            }
        }

        GlowingTextRenderer.setOutlineEnabled(outlined);
        boolean manualOutline = outlined && AngelicaCompatibility.isAngelicaFontRendererActive();
        String[] lines = state.getLines(side);
        int baseY = -lines.length * 5;
        for (int i = 0; i < lines.length; ++i) {
            String line = lines[i];
            if (i == sign.lineBeingEdited) {
                line = "> " + line + " <";
            }
            int lineWidth = fontRenderer.getStringWidth(line);
            int x = -lineWidth / 2;
            int y = i * 10 + baseY;
            if (manualOutline) {
                drawOutlinePasses(fontRenderer, line, x, y);
            }
            fontRenderer.drawString(line, x, y, 0);
        }

        GlowingTextRenderer.setOutlineEnabled(false);
        GL11.glDepthMask(true);

        if (glowing) {
            GL11.glEnable(GL11.GL_LIGHTING);
            if (changedLightmap) {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, baseLightU, baseLightV);
            }
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    /**
     * Outline fallback for Angelica's font renderer. HexText's glyph hooks normally draw the
     * outline per character, but they are not installed when Angelica batches text, so the whole
     * line is redrawn at the eight outline offsets instead. Colour tokens are stripped from the
     * outline passes so the offset copies render in the outline colour, while styles and effects
     * are kept so the outline tracks the shape and motion of the main text.
     */
    private static void drawOutlinePasses(FontRenderer fontRenderer, String line, int x, int y) {
        String outlineText = StringUtils.stripHexColors(line);
        int outlineColor = GlowingTextRenderer.computeOutlineColor(findFirstColor(line));
        for (float[] offset : GlowingTextRenderer.getOutlineOffsets()) {
            fontRenderer.drawString(outlineText, x + (int) offset[0], y + (int) offset[1], outlineColor);
        }
    }

    private static int findFirstColor(String line) {
        ColorCodeUtils.FormattingEnvironment env = ColorCodeUtils.captureFormattingEnvironment(false);
        for (int i = 0; i < line.length(); i++) {
            int codeLen = ColorCodeUtils.detectColorCodeLength(line, i, false, env);
            if (codeLen == 8) {
                char start = line.charAt(i);
                int hexStart = start == '<' ? i + 1 : i + 2;
                int rgb = ColorCodeUtils.parseHexColor(line, hexStart);
                if (rgb != -1) {
                    return rgb;
                }
            } else if (codeLen == 2) {
                char code = line.charAt(i + 1);
                if (ColorCodeUtils.isMinecraftColorCode(code)) {
                    return ColorMath.vanillaColorRgb(ColorCodeUtils.getMinecraftColorIndex(code));
                }
            }
            if (codeLen > 0) {
                i += codeLen - 1;
            }
        }
        return 0;
    }

}
