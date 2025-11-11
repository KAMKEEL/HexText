package kamkeel.hextext.mixin.early.impl.sign;

import kamkeel.hextext.HexText;
import kamkeel.hextext.api.sign.SignSide;
import kamkeel.hextext.common.sign.SignUpdatePacket;
import kamkeel.hextext.common.util.SignTextHelper;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C12PacketUpdateSign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(C12PacketUpdateSign.class)
public abstract class MixinC12PacketUpdateSign extends Packet implements SignUpdatePacket {

    @Shadow
    private String[] field_149590_d;

    @Shadow // X
    private int field_149593_a;

    @Shadow // Y
    private int field_149591_b;

    @Shadow // Z
    private int field_149592_c;

    @Unique
    private SignSide side = SignSide.FRONT;

    @ModifyConstant(method = "readPacketData", constant = @Constant(intValue = 15))
    private int hextext$expandReadLimit(int original) {
        return SignTextHelper.SIGN_LINE_VISIBLE_LIMIT * 2;
    }

    @Inject(method = "writePacketData", at = @At("RETURN"))
    private void hextext$writeSideModified(PacketBuffer data, CallbackInfo ci) {
        if (field_149590_d == null) {
            return;
        }
        if (!HexText.getActiveProxy().isRemoteHexTextPresent()) {
            return;
        }
        data.writeByte(this.side.ordinal());
    }

    @Inject(method = "readPacketData", at = @At("HEAD"), cancellable = true)
    private void hextext$clampPacketLines(PacketBuffer data, CallbackInfo ci) {
        if (!HexText.getActiveProxy().isRemoteHexTextPresent()) {
            return;
        }

        this.field_149593_a = data.readInt();
        this.field_149591_b = data.readShort();
        this.field_149592_c = data.readInt();
        this.field_149590_d = new String[4];

        for (int i = 0; i < 4; ++i) {
            try {
                String raw = data.readStringFromBuffer(SignTextHelper.SIGN_LINE_VISIBLE_LIMIT * 2);
                this.field_149590_d[i] = SignTextHelper.clampToVisibleLimit(raw);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        int b;
        try {
            b = data.readByte(); // we wrote ordinal()
        } catch (IndexOutOfBoundsException e) {
            b = 0; // default to front side if not present
        }
        this.side = (b == SignSide.BACK.ordinal()) ? SignSide.BACK : SignSide.FRONT;

        ci.cancel();
    }

    @Override
    public void setSide(SignSide side) {
        this.side = side;
    }

    @Override
    public SignSide getSide() {
        return this.side;
    }
}
