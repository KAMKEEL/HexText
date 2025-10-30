package kamkeel.hextext.mixin.early.impl.client;

import kamkeel.hextext.client.render.font.GlowingTextRenderer;
import kamkeel.hextext.common.sign.SignSide;
import kamkeel.hextext.common.sign.SignState;
import net.minecraft.block.Block;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySignRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.model.ModelSign;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntitySignRenderer.class)
public abstract class MixinTileEntitySignRenderer extends TileEntitySpecialRenderer {

    @Final
    @Shadow private static ResourceLocation field_147513_b;

    @Final
    @Shadow private ModelSign field_147514_c;

    @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntitySign;DDDF)V", at = @At("HEAD"), cancellable = true)
    private void hextext$renderSign(TileEntitySign sign, double x, double y, double z, float partialTicks, CallbackInfo ci) {
        Block block = sign.getBlockType();
        GL11.glPushMatrix();
        float scale = 0.6666667F;

        if (block == Blocks.standing_sign) {
            GL11.glTranslatef((float) x + 0.5F, (float) y + 0.75F * scale, (float) z + 0.5F);
            float rotation = (float) (sign.getBlockMetadata() * 360) / 16.0F;
            GL11.glRotatef(-rotation, 0.0F, 1.0F, 0.0F);
            this.field_147514_c.signStick.showModel = true;
        } else {
            int metadata = sign.getBlockMetadata();
            float rotation = 0.0F;
            if (metadata == 2) {
                rotation = 180.0F;
            }
            if (metadata == 4) {
                rotation = 90.0F;
            }
            if (metadata == 5) {
                rotation = -90.0F;
            }
            GL11.glTranslatef((float) x + 0.5F, (float) y + 0.75F * scale, (float) z + 0.5F);
            GL11.glRotatef(-rotation, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(0.0F, -0.3125F, -0.4375F);
            this.field_147514_c.signStick.showModel = false;
        }

        this.bindTexture(field_147513_b);
        GL11.glPushMatrix();
        GL11.glScalef(scale, -scale, -scale);
        this.field_147514_c.renderSign();
        GL11.glPopMatrix();

        FontRenderer fontRenderer = this.func_147498_b();
        SignState state = (SignState) sign;

        renderTextForSide(sign, state, fontRenderer, SignSide.FRONT, scale, true);
        renderTextForSide(sign, state, fontRenderer, SignSide.BACK, scale, false);

        GL11.glPopMatrix();
        ci.cancel();
    }

    @Unique
    private void renderTextForSide(TileEntitySign sign, SignState state, FontRenderer fontRenderer,
            SignSide side, float scale, boolean front) {
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 0.5F * scale, front ? 0.07F * scale : -0.07F * scale);
        if (!front) {
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
        }

        float fontScale = 0.016666668F * scale;
        GL11.glScalef(fontScale, -fontScale, fontScale);
        GL11.glNormal3f(0.0F, 0.0F, -1.0F * fontScale);
        GL11.glDepthMask(false);

        boolean glowing = state.hextext$isGlowing(side);
        boolean outlined = state.hextext$isOutlined(side);
        boolean lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
        if (glowing && lightingEnabled) {
            GL11.glDisable(GL11.GL_LIGHTING);
        }
        GlowingTextRenderer.setOutlineEnabled(glowing || outlined);

        String[] lines = state.hextext$getLines(side);
        int baseY = -lines.length * 5;
        SignSide editingSide = state.hextext$getEditingSide();
        for (int i = 0; i < lines.length; ++i) {
            String line = lines[i];
            if (editingSide == side && i == sign.lineBeingEdited) {
                line = "> " + line + " <";
            }
            fontRenderer.drawString(line, -fontRenderer.getStringWidth(line) / 2, i * 10 + baseY, 0);
        }

        GlowingTextRenderer.setOutlineEnabled(false);
        if (glowing && lightingEnabled) {
            GL11.glEnable(GL11.GL_LIGHTING);
        }
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

}
