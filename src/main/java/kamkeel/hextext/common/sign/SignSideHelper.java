package kamkeel.hextext.common.sign;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.MathHelper;

public final class SignSideHelper {

    private SignSideHelper() {
    }

    public static SignSide determineSide(TileEntitySign sign, double playerX, double playerZ,
            float hitX, float hitZ) {
        double centerX = sign.xCoord + 0.5D;
        double centerZ = sign.zCoord + 0.5D;

        double[] normal = computeFrontNormal(sign.getBlockType(), sign.getBlockMetadata());

        double vecX;
        double vecZ;

        if (sign.getBlockType() == Blocks.standing_sign) {
            vecX = hitX - 0.5D;
            vecZ = hitZ - 0.5D;

            if (Math.abs(vecX) < 1.0E-6D && Math.abs(vecZ) < 1.0E-6D) {
                vecX = playerX - centerX;
                vecZ = playerZ - centerZ;
            }
        } else {
            vecX = playerX - centerX;
            vecZ = playerZ - centerZ;
        }

        if (vecX == 0.0D && vecZ == 0.0D) {
            return SignSide.FRONT;
        }

        double dot = vecX * normal[0] + vecZ * normal[1];
        return dot >= 0.0D ? SignSide.FRONT : SignSide.BACK;
    }

    private static double[] computeFrontNormal(Block block, int metadata) {
        if (block == Blocks.wall_sign) {
            switch (metadata) {
                case 2:
                    return new double[] {0.0D, -1.0D};
                case 3:
                    return new double[] {0.0D, 1.0D};
                case 4:
                    return new double[] {-1.0D, 0.0D};
                case 5:
                    return new double[] {1.0D, 0.0D};
                default:
                    return new double[] {0.0D, 1.0D};
            }
        }

        float wrapped = MathHelper.wrapAngleTo180_float((float) (metadata * 360) / 16.0F);
        double radians = Math.toRadians(wrapped);
        return new double[] {MathHelper.sin((float) radians), MathHelper.cos((float) radians)};
    }
}
