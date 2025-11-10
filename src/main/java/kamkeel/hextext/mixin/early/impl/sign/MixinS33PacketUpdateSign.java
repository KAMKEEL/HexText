package kamkeel.hextext.mixin.early.impl.sign;

import kamkeel.hextext.api.sign.SignSide;
import kamkeel.hextext.common.sign.SignSyncPacket;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S33PacketUpdateSign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(S33PacketUpdateSign.class)
public abstract class MixinS33PacketUpdateSign extends Packet implements SignSyncPacket {

    @Unique
    private String[] hextext$backLines = new String[]{"", "", "", ""};

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
        if (!data.isReadable()) {
            hextext$backLines = new String[]{"", "", "", ""};
            hextext$glowFront = false;
            hextext$glowBack = false;
            hextext$outlineFront = false;
            hextext$outlineBack = false;
            hextext$waxed = false;
            return;
        }

        int startIndex = data.readerIndex();
        String[] lines = new String[4];
        try {
            for (int i = 0; i < lines.length; i++) {
                lines[i] = data.readStringFromBuffer(50);
            }
            hextext$backLines = lines;

            if (data.readableBytes() >= 5) {
                hextext$glowFront = data.readBoolean();
                hextext$glowBack = data.readBoolean();
                hextext$outlineFront = data.readBoolean();
                hextext$outlineBack = data.readBoolean();
                hextext$waxed = data.readBoolean();
            } else {
                hextext$glowFront = false;
                hextext$glowBack = false;
                hextext$outlineFront = false;
                hextext$outlineBack = false;
                hextext$waxed = false;
            }
        } catch (IndexOutOfBoundsException | IOException exception) {
            data.readerIndex(startIndex);
            hextext$backLines = new String[]{"", "", "", ""};
            hextext$glowFront = false;
            hextext$glowBack = false;
            hextext$outlineFront = false;
            hextext$outlineBack = false;
            hextext$waxed = false;
        }
    }

    @Override
    public void setBackText(String[] lines) {
        if (lines == null) {
            hextext$backLines = new String[]{"", "", "", ""};
        } else {
            hextext$backLines = lines.clone();
        }
    }

    @Override
    public String[] getBackText() {
        return hextext$backLines.clone();
    }

    @Override
    public void setGlowing(SignSide side, boolean glowing) {
        if (side == SignSide.FRONT) {
            hextext$glowFront = glowing;
        } else {
            hextext$glowBack = glowing;
        }
    }

    @Override
    public boolean isGlowing(SignSide side) {
        return side == SignSide.FRONT ? hextext$glowFront : hextext$glowBack;
    }

    @Override
    public void setOutlined(SignSide side, boolean outlined) {
        if (side == SignSide.FRONT) {
            hextext$outlineFront = outlined;
        } else {
            hextext$outlineBack = outlined;
        }
    }

    @Override
    public boolean isOutlined(SignSide side) {
        return side == SignSide.FRONT ? hextext$outlineFront : hextext$outlineBack;
    }

    @Override
    public void setWaxed(boolean waxed) {
        hextext$waxed = waxed;
    }

    @Override
    public boolean isWaxed() {
        return hextext$waxed;
    }
}
