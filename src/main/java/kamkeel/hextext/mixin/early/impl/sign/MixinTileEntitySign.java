package kamkeel.hextext.mixin.early.impl.sign;

import kamkeel.hextext.common.sign.SignSide;
import kamkeel.hextext.common.sign.IHexTextSign;
import kamkeel.hextext.common.sign.SignSyncPacket;
import kamkeel.hextext.common.util.SignTextHelper;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S33PacketUpdateSign;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TileEntitySign.class)
public abstract class MixinTileEntitySign extends TileEntity implements IHexTextSign {


    @Unique
    private boolean isWaxed = false;

    @Shadow
    public abstract void setEditable(boolean p_145913_1_);

    // Front Text
    @Shadow
    public String[] signText;

    @Unique
    private boolean[] glowStates = new boolean[SignSide.values().length];

    @Unique
    private boolean[] outlineStates = new boolean[SignSide.values().length];

    @Unique
    private String[] backSignText = new String[] {"", "", "", ""};

    @Unique SignSide editSide = SignSide.FRONT;

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void hextext$clampLoadedLines(NBTTagCompound compound, CallbackInfo ci) {
        if (signText != null) {
            hextext$clampLines(signText);
        }
        isWaxed = compound.getBoolean("HexTextWaxed");
        glowStates[SignSide.FRONT.ordinal()] = compound.getBoolean("HexTextGlowFront");
        glowStates[SignSide.BACK.ordinal()] = compound.getBoolean("HexTextGlowBack");
        outlineStates[SignSide.FRONT.ordinal()] = compound.getBoolean("HexTextOutlineFront");
        outlineStates[SignSide.BACK.ordinal()] = compound.getBoolean("HexTextOutlineBack");
        if (compound.hasKey("HexTextBackText0")) {
            for (int i = 0; i < backSignText.length; i++) {
                backSignText[i] = compound.getString("HexTextBackText" + i);
            }
        }
        hextext$clampLines(backSignText);

        // Allows Client to Edit Sign
        setEditable(!isWaxed);
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void hextext$writeCustomData(NBTTagCompound compound, CallbackInfo ci) {
        compound.setBoolean("HexTextWaxed", isWaxed);
        compound.setBoolean("HexTextGlowFront", glowStates[SignSide.FRONT.ordinal()]);
        compound.setBoolean("HexTextGlowBack", glowStates[SignSide.BACK.ordinal()]);
        compound.setBoolean("HexTextOutlineFront", outlineStates[SignSide.FRONT.ordinal()]);
        compound.setBoolean("HexTextOutlineBack", outlineStates[SignSide.BACK.ordinal()]);
        for (int i = 0; i < backSignText.length; i++) {
            compound.setString("HexTextBackText" + i, backSignText[i]);
        }
    }

    @Override
    @Unique
    public boolean isWaxed() {
        return isWaxed;
    }

    @Override
    @Unique
    public void setWaxed(boolean waxed) {
        if (isWaxed != waxed) {
            isWaxed = waxed;
            markDirty();
        }
    }

    @Override
    @Unique
    public boolean isGlowing(SignSide side) {
        return glowStates[side.ordinal()];
    }

    @Override
    @Unique
    public boolean setGlowing(SignSide side, boolean glowing) {
        int index = side.ordinal();
        boolean changed = glowStates[index] != glowing;
        glowStates[index] = glowing;
        if (changed) {
            markDirty();
        }
        return changed;
    }

    @Override
    @Unique
    public boolean isOutlined(SignSide side) {
        return outlineStates[side.ordinal()];
    }

    @Override
    @Unique
    public boolean setOutlined(SignSide side, boolean outlined) {
        int index = side.ordinal();
        boolean changed = outlineStates[index] != outlined;
        outlineStates[index] = outlined;
        if (changed) {
            markDirty();
        }
        return changed;
    }

    @Override
    @Unique
    public void setEditSide(SignSide side){
        this.editSide = side;
    }

    @Override
    @Unique
    public SignSide getEditSide(){
        return this.editSide;
    }

    @Override
    @Unique
    public String[] getLines(SignSide side) {
        return side == SignSide.FRONT ? signText : backSignText;
    }

    @Inject(method = "getDescriptionPacket", at = @At("RETURN"))
    private void hextext$appendCustomSyncData(CallbackInfoReturnable<Packet> cir) {
        Packet packet = cir.getReturnValue();
        if (!(packet instanceof S33PacketUpdateSign)) {
            return;
        }

        SignSyncPacket sync = (SignSyncPacket) packet;
        sync.setBackText(backSignText);
        sync.setGlowing(SignSide.FRONT, glowStates[SignSide.FRONT.ordinal()]);
        sync.setGlowing(SignSide.BACK, glowStates[SignSide.BACK.ordinal()]);
        sync.setOutlined(SignSide.FRONT, outlineStates[SignSide.FRONT.ordinal()]);
        sync.setOutlined(SignSide.BACK, outlineStates[SignSide.BACK.ordinal()]);
        sync.setWaxed(isWaxed);
    }

    @Unique
    private void hextext$clampLines(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            lines[i] = SignTextHelper.clampToVisibleLimit(lines[i]);
        }
    }
}
