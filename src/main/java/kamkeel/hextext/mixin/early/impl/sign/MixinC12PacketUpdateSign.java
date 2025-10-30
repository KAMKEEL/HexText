package kamkeel.hextext.mixin.early.impl.sign;

import kamkeel.hextext.common.sign.SignSide;
import kamkeel.hextext.common.sign.SignSyncPacket;
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
        data.writeByte(this.side.ordinal());
    }

    @Inject(method = "readPacketData", at = @At("HEAD"), cancellable = true)
    private void hextext$clampPacketLines(PacketBuffer data, CallbackInfo ci) {
        this.field_149593_a = data.readInt();
        this.field_149591_b = data.readShort();
        this.field_149592_c = data.readInt();
        this.field_149590_d = new String[4];

        for (int i = 0; i < 4; ++i)
        {
            field_149590_d[i] = SignTextHelper.clampToVisibleLimit(field_149590_d[i]);
        }

        this.side = SignSide.fromBoolean(data.readByte() == 0);
        ci.cancel();
    }

    public void setSide(SignSide side){
        this.side = side;
    }
}
