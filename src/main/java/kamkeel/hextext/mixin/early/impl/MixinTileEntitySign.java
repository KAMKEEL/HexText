package kamkeel.hextext.mixin.early.impl;

import kamkeel.hextext.common.sign.SignSide;
import kamkeel.hextext.common.sign.SignState;
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

@Mixin(TileEntitySign.class)
public abstract class MixinTileEntitySign extends TileEntity implements SignState {


    @Unique
    private boolean hextext$isWaxed = false;

    @Shadow
    public String[] signText;

    @Unique
    private final boolean[] hextext$glowStates = new boolean[SignSide.values().length];

    @Unique
    private final String[] hextext$backSignText = new String[] {"", "", "", ""};

    @Unique
    private SignSide hextext$editingSide = SignSide.FRONT;

    @Unique
    private String[] hextext$frontBackup = null;

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void hextext$clampLoadedLines(NBTTagCompound compound, CallbackInfo ci) {
        if (signText != null) {
            hextext$clampLines(signText);
        }
        hextext$isWaxed = compound.getBoolean("HexTextWaxed");
        hextext$glowStates[SignSide.FRONT.ordinal()] = compound.getBoolean("HexTextGlowFront");
        hextext$glowStates[SignSide.BACK.ordinal()] = compound.getBoolean("HexTextGlowBack");
        if (compound.hasKey("HexTextBackText0")) {
            for (int i = 0; i < hextext$backSignText.length; i++) {
                hextext$backSignText[i] = compound.getString("HexTextBackText" + i);
            }
        }
        hextext$clampLines(hextext$backSignText);
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void hextext$writeCustomData(NBTTagCompound compound, CallbackInfo ci) {
        compound.setBoolean("HexTextWaxed", hextext$isWaxed);
        compound.setBoolean("HexTextGlowFront", hextext$glowStates[SignSide.FRONT.ordinal()]);
        compound.setBoolean("HexTextGlowBack", hextext$glowStates[SignSide.BACK.ordinal()]);
        for (int i = 0; i < hextext$backSignText.length; i++) {
            compound.setString("HexTextBackText" + i, hextext$backSignText[i]);
        }
    }

    @Override
    public SignSide hextext$getEditingSide() {
        return hextext$editingSide;
    }

    @Override
    public void hextext$setEditingSide(SignSide side) {
        this.hextext$editingSide = side == null ? SignSide.FRONT : side;
    }

    @Override
    public void hextext$prepareForEdit(SignSide side) {
        hextext$editingSide = side;
        if (side == SignSide.BACK) {
            hextext$frontBackup = signText.clone();
            hextext$copyLines(hextext$backSignText, signText);
        } else {
            hextext$frontBackup = null;
        }
    }

    @Override
    public void hextext$finishEdit() {
        if (hextext$editingSide == SignSide.BACK) {
            hextext$clampLines(signText);
            hextext$copyLines(signText, hextext$backSignText);
            if (hextext$frontBackup != null) {
                hextext$copyLines(hextext$frontBackup, signText);
            }
        } else {
            hextext$clampLines(signText);
        }
        hextext$frontBackup = null;
        hextext$editingSide = SignSide.FRONT;
        markDirty();
    }

    @Override
    public boolean hextext$isWaxed() {
        return hextext$isWaxed;
    }

    @Override
    public void hextext$setWaxed(boolean waxed) {
        if (hextext$isWaxed != waxed) {
            hextext$isWaxed = waxed;
            markDirty();
        }
    }

    @Override
    public boolean hextext$isGlowing(SignSide side) {
        return hextext$glowStates[side.ordinal()];
    }

    @Override
    public boolean hextext$setGlowing(SignSide side, boolean glowing) {
        int index = side.ordinal();
        boolean changed = hextext$glowStates[index] != glowing;
        hextext$glowStates[index] = glowing;
        if (changed) {
            markDirty();
        }
        return changed;
    }

    @Override
    public String[] hextext$getLines(SignSide side) {
        return side == SignSide.FRONT ? signText : hextext$backSignText;
    }

    @Override
    public Packet getDescriptionPacket() {
        String[] copy = new String[signText.length];
        System.arraycopy(signText, 0, copy, 0, signText.length);
        S33PacketUpdateSign packet = new S33PacketUpdateSign(xCoord, yCoord, zCoord, copy);
        SignSyncPacket syncPacket = (SignSyncPacket) packet;
        syncPacket.hextext$setBackText(hextext$backSignText.clone());
        for (SignSide side : SignSide.values()) {
            syncPacket.hextext$setGlowing(side, hextext$glowStates[side.ordinal()]);
        }
        syncPacket.hextext$setWaxed(hextext$isWaxed);
        return packet;
    }

    @Unique
    private void hextext$copyLines(String[] source, String[] target) {
        System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
    }

    @Unique
    private void hextext$clampLines(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            lines[i] = SignTextHelper.clampToVisibleLimit(lines[i]);
        }
    }
}
