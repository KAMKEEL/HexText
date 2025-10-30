package kamkeel.hextext.common.sign;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.MathHelper;

public final class SignSideHelper {

    private SignSideHelper() {
    }

    public static SignSide determineSide(TileEntitySign sign, double playerX, double playerZ,
            float playerYaw, float hitX, float hitZ) {
        double centerX = sign.xCoord + 0.5D;
        double centerZ = sign.zCoord + 0.5D;

        Block block = sign.getBlockType();
        double[] normal = computeFrontNormal(block, sign.getBlockMetadata());

        if (block == Blocks.standing_sign) {
            double hitVecX = hitX - 0.5D;
            double hitVecZ = hitZ - 0.5D;
            double hitDot = hitVecX * normal[0] + hitVecZ * normal[1];

            if (Math.abs(hitDot) > 1.0E-3D) {
                return hitDot >= 0.0D ? SignSide.FRONT : SignSide.BACK;
            }
        }

        double vecX = playerX - centerX;
        double vecZ = playerZ - centerZ;

        double magnitudeSq = vecX * vecX + vecZ * vecZ;
        if (block == Blocks.standing_sign && magnitudeSq < 1.0E-4D) {
            return determineFromFacing(normal, playerYaw);
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

    private static SignSide determineFromFacing(double[] normal, float playerYaw) {
        double frontYaw = Math.toDegrees(Math.atan2(normal[0], normal[1]));
        float relative = MathHelper.wrapAngleTo180_float(playerYaw - (float) frontYaw);
        return Math.abs(relative) <= 90.0F ? SignSide.FRONT : SignSide.BACK;
    }
}
