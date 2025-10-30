package kamkeel.hextext.common.sign;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;

public final class SignSideHelper {

    private SignSideHelper() {
    }

    public static SignSide determineSide(TileEntitySign sign, double playerX, double playerZ,
            float hitX, float hitZ) {
        double centerX = sign.xCoord + 0.5D;
        double centerZ = sign.zCoord + 0.5D;

        Block block = sign.getBlockType();
        int metadata = sign.getBlockMetadata();

        double vecX = playerX - centerX;
        double vecZ = playerZ - centerZ;

        if (block == Blocks.standing_sign) {
            if (isNearlyZero(vecX) && isNearlyZero(vecZ)) {
                vecX = hitX - 0.5D;
                vecZ = hitZ - 0.5D;
            }

            if (isNearlyZero(vecX) && isNearlyZero(vecZ)) {
                return SignSide.FRONT;
            }

            return isFacingFrontStanding(vecX, vecZ, metadata) ? SignSide.FRONT : SignSide.BACK;
        }

        double[] normal = computeWallFrontNormal(metadata);

        if (isNearlyZero(vecX) && isNearlyZero(vecZ)) {
            vecX = normal[0];
            vecZ = normal[1];
        }

        double dot = vecX * normal[0] + vecZ * normal[1];
        return dot >= 0.0D ? SignSide.FRONT : SignSide.BACK;
    }

    private static boolean isFacingFrontStanding(double vecX, double vecZ, int metadata) {
        double playerAngle = Math.atan2(vecZ, vecX);
        double facingAngle = computeStandingFacingAngle(metadata);
        double delta = normalizeRadians(playerAngle - facingAngle);
        return Math.abs(delta) <= Math.PI / 2.0D;
    }

    private static double computeStandingFacingAngle(int metadata) {
        double rotationDegrees = (metadata * 360.0D) / 16.0D;
        double radians = Math.toRadians(rotationDegrees);
        return normalizeRadians(radians + (Math.PI / 2.0D));
    }

    private static double[] computeWallFrontNormal(int metadata) {
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

    private static double normalizeRadians(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    private static boolean isNearlyZero(double value) {
        return Math.abs(value) < 1.0E-6D;
    }
}
