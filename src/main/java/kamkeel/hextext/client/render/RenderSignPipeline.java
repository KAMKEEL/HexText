package kamkeel.hextext.client.render;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.client.render.font.GlowingTextRenderer;
import kamkeel.hextext.api.sign.IHexTextSign;
import kamkeel.hextext.api.sign.SignSide;
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
        String[] lines = state.getLines(side);
        int baseY = -lines.length * 5;
        for (int i = 0; i < lines.length; ++i) {
            String line = lines[i];
            if (i == sign.lineBeingEdited) {
                line = "> " + line + " <";
            }
            int lineWidth = fontRenderer.getStringWidth(line);
            fontRenderer.drawString(line, -lineWidth / 2, i * 10 + baseY, 0);
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

}
