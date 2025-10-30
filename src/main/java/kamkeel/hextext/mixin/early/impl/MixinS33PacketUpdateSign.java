package kamkeel.hextext.mixin.early.impl;

import kamkeel.hextext.common.sign.SignSide;
import kamkeel.hextext.common.sign.SignSyncPacket;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S33PacketUpdateSign;

import java.io.IOException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(S33PacketUpdateSign.class)
public abstract class MixinS33PacketUpdateSign extends Packet implements SignSyncPacket {

    @Shadow private int field_149352_a;
    @Shadow private int field_149350_b;
    @Shadow private int field_149351_c;

    @Unique
    private String[] hextext$backLines = new String[] {"", "", "", ""};

    @Unique
    private boolean hextext$glowFront;

    @Unique
    private boolean hextext$glowBack;

    @Unique
    private boolean hextext$outlineFront;

    @Unique
    private boolean hextext$outlineBack;

    @Unique
    private boolean hextext$waxed;

    @Inject(method = "writePacketData", at = @At("TAIL"))
    private void hextext$writeExtras(PacketBuffer data, CallbackInfo ci) {
        for (String line : hextext$backLines) {
            try {
                data.writeStringToBuffer(line);
            } catch (IOException exception) {
                throw new RuntimeException("Failed to write HexText back text", exception);
            }
        }
        data.writeBoolean(hextext$glowFront);
        data.writeBoolean(hextext$glowBack);
        data.writeBoolean(hextext$outlineFront);
        data.writeBoolean(hextext$outlineBack);
        data.writeBoolean(hextext$waxed);
    }

    @Inject(method = "readPacketData", at = @At("TAIL"))
    private void hextext$readExtras(PacketBuffer data, CallbackInfo ci) {
        hextext$backLines = new String[4];
        for (int i = 0; i < hextext$backLines.length; i++) {
            try {
                hextext$backLines[i] = data.readStringFromBuffer(50);
            } catch (IOException exception) {
                throw new RuntimeException("Failed to read HexText back text", exception);
            }
        }
        hextext$glowFront = data.readBoolean();
        hextext$glowBack = data.readBoolean();
        hextext$outlineFront = data.readBoolean();
        hextext$outlineBack = data.readBoolean();
        hextext$waxed = data.readBoolean();
    }

    @Override
    public void hextext$setBackText(String[] lines) {
        if (lines == null) {
            hextext$backLines = new String[] {"", "", "", ""};
        } else {
            hextext$backLines = lines.clone();
        }
    }

    @Override
    public String[] hextext$getBackText() {
        return hextext$backLines.clone();
    }

    @Override
    public void hextext$setGlowing(SignSide side, boolean glowing) {
        if (side == SignSide.FRONT) {
            hextext$glowFront = glowing;
        } else {
            hextext$glowBack = glowing;
        }
    }

    @Override
    public boolean hextext$isGlowing(SignSide side) {
        return side == SignSide.FRONT ? hextext$glowFront : hextext$glowBack;
    }

    @Override
    public void hextext$setOutlined(SignSide side, boolean outlined) {
        if (side == SignSide.FRONT) {
            hextext$outlineFront = outlined;
        } else {
            hextext$outlineBack = outlined;
        }
    }

    @Override
    public boolean hextext$isOutlined(SignSide side) {
        return side == SignSide.FRONT ? hextext$outlineFront : hextext$outlineBack;
    }

    @Override
    public void hextext$setWaxed(boolean waxed) {
        hextext$waxed = waxed;
    }

    @Override
    public boolean hextext$isWaxed() {
        return hextext$waxed;
    }
}
