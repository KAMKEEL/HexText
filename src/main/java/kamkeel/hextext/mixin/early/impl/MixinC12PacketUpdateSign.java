package kamkeel.hextext.mixin.early.impl;

import kamkeel.hextext.common.sign.SignSide;
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

    @Unique
    private static final int HEXTEXT_SIGN_PACKET_LIMIT = 50;

    @Shadow
    private String[] field_149590_d;

    @Unique
    private SignSide hextext$editingSide = SignSide.FRONT;

    @ModifyConstant(method = "readPacketData", constant = @Constant(intValue = 15))
    private int hextext$expandReadLimit(int original) {
        return HEXTEXT_SIGN_PACKET_LIMIT;
    }

    @Inject(method = "readPacketData", at = @At("RETURN"))
    private void hextext$clampPacketLines(PacketBuffer data, CallbackInfo ci) {
        if (field_149590_d == null) {
            return;
        }
        for (int i = 0; i < field_149590_d.length; i++) {
            field_149590_d[i] = SignTextHelper.clampToVisibleLimit(field_149590_d[i]);
        }
    }

    @Inject(method = "writePacketData", at = @At("TAIL"))
    private void hextext$writeSide(PacketBuffer data, CallbackInfo ci) {
        data.writeByte(hextext$editingSide.ordinal());
    }

    @Inject(method = "readPacketData", at = @At("TAIL"))
    private void hextext$readSide(PacketBuffer data, CallbackInfo ci) {
        int ordinal = data.readByte();
        if (ordinal < 0 || ordinal >= SignSide.values().length) {
            hextext$editingSide = SignSide.FRONT;
        } else {
            hextext$editingSide = SignSide.values()[ordinal];
        }
    }

    @Override
    @Unique
    public void hextext$setEditingSide(SignSide side) {
        this.hextext$editingSide = side == null ? SignSide.FRONT : side;
    }

    @Override
    @Unique
    public SignSide hextext$getEditingSide() {
        return hextext$editingSide;
    }
}
