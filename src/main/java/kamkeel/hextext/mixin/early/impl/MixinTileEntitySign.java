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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(TileEntitySign.class)
public abstract class MixinTileEntitySign extends TileEntity implements SignState {

    @Unique
    private static final int hextext$SIGN_LINE_COUNT = 4;

    @Unique
    private boolean hextext$isWaxed = false;

    @Shadow
    public abstract void setEditable(boolean canEdit);

    @Shadow
    public String[] signText;

    @Unique
    private final boolean[] hextext$glowStates = new boolean[SignSide.values().length];

    @Unique
    private final boolean[] hextext$outlineStates = new boolean[SignSide.values().length];

    @Unique
    private final String[] hextext$backSignText = hextext$createEmptyLines();

    @Unique
    private SignSide hextext$editingSide = SignSide.FRONT;

    @Unique
    private String[] hextext$frontSnapshot;

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void hextext$readExtraData(NBTTagCompound compound, CallbackInfo ci) {
        if (signText != null) {
            hextext$clampLines(signText);
        }

        hextext$isWaxed = compound.getBoolean("HexTextWaxed");
        hextext$glowStates[SignSide.FRONT.ordinal()] = compound.getBoolean("HexTextGlowFront");
        hextext$glowStates[SignSide.BACK.ordinal()] = compound.getBoolean("HexTextGlowBack");
        hextext$outlineStates[SignSide.FRONT.ordinal()] = compound.getBoolean("HexTextOutlineFront");
        hextext$outlineStates[SignSide.BACK.ordinal()] = compound.getBoolean("HexTextOutlineBack");

        Arrays.fill(hextext$backSignText, "");
        if (compound.hasKey("HexTextBackText0")) {
            for (int line = 0; line < hextext$backSignText.length; line++) {
                hextext$backSignText[line] = compound.getString("HexTextBackText" + line);
            }
        }
        hextext$clampLines(hextext$backSignText);

        setEditable(!hextext$isWaxed);
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void hextext$writeExtraData(NBTTagCompound compound, CallbackInfo ci) {
        compound.setBoolean("HexTextWaxed", hextext$isWaxed);
        compound.setBoolean("HexTextGlowFront", hextext$glowStates[SignSide.FRONT.ordinal()]);
        compound.setBoolean("HexTextGlowBack", hextext$glowStates[SignSide.BACK.ordinal()]);
        compound.setBoolean("HexTextOutlineFront", hextext$outlineStates[SignSide.FRONT.ordinal()]);
        compound.setBoolean("HexTextOutlineBack", hextext$outlineStates[SignSide.BACK.ordinal()]);

        for (int line = 0; line < hextext$backSignText.length; line++) {
            compound.setString("HexTextBackText" + line, hextext$backSignText[line]);
        }
    }

    @Override
    @Unique
    public SignSide hextext$getEditingSide() {
        return hextext$editingSide;
    }

    @Override
    @Unique
    public void hextext$setEditingSide(SignSide side) {
        hextext$editingSide = side == null ? SignSide.FRONT : side;
    }

    @Override
    @Unique
    public void hextext$prepareForEdit(SignSide side) {
        hextext$editingSide = side == null ? SignSide.FRONT : side;

        if (hextext$isEditing(SignSide.BACK)) {
            hextext$frontSnapshot = signText.clone();
            hextext$copyLines(hextext$backSignText, signText);
        } else {
            hextext$frontSnapshot = null;
        }
    }

    @Override
    @Unique
    public void hextext$finishEdit() {
        if (hextext$isEditing(SignSide.BACK)) {
            hextext$clampLines(signText);
            hextext$copyLines(signText, hextext$backSignText);
            hextext$restoreFrontPreview();
        } else {
            hextext$clampLines(signText);
        }

        hextext$editingSide = SignSide.FRONT;
        markDirty();
    }

    @Override
    @Unique
    public boolean hextext$isWaxed() {
        return hextext$isWaxed;
    }

    @Override
    @Unique
    public void hextext$setWaxed(boolean waxed) {
        if (hextext$isWaxed != waxed) {
            hextext$isWaxed = waxed;
            setEditable(!hextext$isWaxed);
            markDirty();
        }
    }

    @Override
    @Unique
    public boolean hextext$isGlowing(SignSide side) {
        return hextext$glowStates[side.ordinal()];
    }

    @Override
    @Unique
    public boolean hextext$setGlowing(SignSide side, boolean glowing) {
        int index = side.ordinal();
        boolean changed = hextext$glowStates[index] != glowing;
        if (changed) {
            hextext$glowStates[index] = glowing;
            markDirty();
        }
        return changed;
    }

    @Override
    @Unique
    public boolean hextext$isOutlined(SignSide side) {
        return hextext$outlineStates[side.ordinal()];
    }

    @Override
    @Unique
    public boolean hextext$setOutlined(SignSide side, boolean outlined) {
        int index = side.ordinal();
        boolean changed = hextext$outlineStates[index] != outlined;
        if (changed) {
            hextext$outlineStates[index] = outlined;
            markDirty();
        }
        return changed;
    }

    @Override
    @Unique
    public String[] hextext$getLines(SignSide side) {
        if (hextext$isEditing(SignSide.BACK)) {
            return side == SignSide.BACK ? signText : hextext$getFrontPreview();
        }

        return hextext$getStoredLines(side);
    }

    @Inject(method = "getDescriptionPacket", at = @At("RETURN"))
    private void hextext$appendCustomSyncData(CallbackInfoReturnable<Packet> cir) {
        Packet packet = cir.getReturnValue();
        if (!(packet instanceof S33PacketUpdateSign)) {
            return;
        }

        SignSyncPacket sync = (SignSyncPacket) packet;
        sync.hextext$setBackText(hextext$backSignText);
        sync.hextext$setGlowing(SignSide.FRONT, hextext$glowStates[SignSide.FRONT.ordinal()]);
        sync.hextext$setGlowing(SignSide.BACK, hextext$glowStates[SignSide.BACK.ordinal()]);
        sync.hextext$setOutlined(SignSide.FRONT, hextext$outlineStates[SignSide.FRONT.ordinal()]);
        sync.hextext$setOutlined(SignSide.BACK, hextext$outlineStates[SignSide.BACK.ordinal()]);
        sync.hextext$setWaxed(hextext$isWaxed);
    }

    @Unique
    private boolean hextext$isEditing(SignSide side) {
        return hextext$editingSide == side;
    }

    @Unique
    private String[] hextext$getStoredLines(SignSide side) {
        return side == SignSide.FRONT ? signText : hextext$backSignText;
    }

    @Unique
    private String[] hextext$getFrontPreview() {
        return hextext$frontSnapshot != null ? hextext$frontSnapshot : signText;
    }

    @Unique
    private void hextext$restoreFrontPreview() {
        if (hextext$frontSnapshot != null) {
            hextext$copyLines(hextext$frontSnapshot, signText);
            hextext$frontSnapshot = null;
        }
    }

    @Unique
    private static String[] hextext$createEmptyLines() {
        String[] lines = new String[hextext$SIGN_LINE_COUNT];
        Arrays.fill(lines, "");
        return lines;
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
