package kamkeel.hextext.common.sign;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;
import org.junit.Assert;
import org.junit.Test;

public class SignSideHelperTest {

    @Test
    public void standingSignDeterminesSideByAngle() {
        assertStandingFrontBack(0);
        assertStandingFrontBack(4);
        assertStandingFrontBack(8);
        assertStandingFrontBack(12);
        assertStandingFrontBack(1);
        assertStandingFrontBack(9);
    }

    @Test
    public void wallSignsContinueToUsePlayerPosition() {
        TestSign sign = new TestSign(Blocks.wall_sign, 2, 3, 3);

        SignSide wallFront = SignSideHelper.determineSide(sign, 3.5D, 1.0D, 0.5F, 0.5F);
        SignSide wallBack = SignSideHelper.determineSide(sign, 3.5D, 5.0D, 0.5F, 0.5F);

        Assert.assertNotEquals(wallFront, wallBack);
    }

    @Test
    public void hitCoordinatesUsedWhenPlayerVectorZero() {
        TestSign sign = new TestSign(Blocks.standing_sign, 4, 50, 50);

        SignSide frontHit = SignSideHelper.determineSide(sign, 50.5D, 50.5D, 0.1F, 0.5F);
        SignSide backHit = SignSideHelper.determineSide(sign, 50.5D, 50.5D, 0.9F, 0.5F);

        Assert.assertEquals(SignSide.FRONT, frontHit);
        Assert.assertEquals(SignSide.BACK, backHit);
    }

    private void assertStandingFrontBack(int metadata) {
        TestSign sign = new TestSign(Blocks.standing_sign, metadata, 10 + metadata, 20 + metadata);
        double[] frontVector = computeFrontVector(metadata);
        double centerX = sign.xCoord + 0.5D;
        double centerZ = sign.zCoord + 0.5D;

        SignSide front = SignSideHelper.determineSide(sign, centerX + frontVector[0], centerZ + frontVector[1], 0.5F, 0.5F);
        SignSide back = SignSideHelper.determineSide(sign, centerX - frontVector[0], centerZ - frontVector[1], 0.5F, 0.5F);

        Assert.assertEquals("Metadata " + metadata + " should treat the front vector as front", SignSide.FRONT, front);
        Assert.assertEquals("Metadata " + metadata + " should treat the opposite vector as back", SignSide.BACK, back);
    }

    private double[] computeFrontVector(int metadata) {
        double rotationDegrees = (metadata * 360.0D) / 16.0D;
        double radians = Math.toRadians(rotationDegrees);
        double facing = radians + (Math.PI / 2.0D);
        double normalized = Math.atan2(Math.sin(facing), Math.cos(facing));
        return new double[] {Math.cos(normalized), Math.sin(normalized)};
    }

    private static final class TestSign extends TileEntitySign {
        private final Block block;
        private final int metadata;

        private TestSign(Block block, int metadata, int x, int z) {
            this.block = block;
            this.metadata = metadata;
            this.xCoord = x;
            this.yCoord = 0;
            this.zCoord = z;
        }

        @Override
        public Block getBlockType() {
            return block;
        }

        @Override
        public int getBlockMetadata() {
            return metadata;
        }
    }
}
