package oxidizing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import oxidizing.config.ConfigIO;

// What ChangeOverTimeBlockMixin runs on each oxidation roll, kept out of the mixin so the GameTests can call it directly
public final class WetOxidation {

    private static final int ALL_SIDES = 6;

    private WetOxidation() {}

    /** Number of faces touching water (source, flowing or waterlogged neighbour); a waterlogged block counts as all 6 */
    public static int wetSides(BlockState state, Level level, BlockPos pos) {
        if (state.getFluidState().is(FluidTags.WATER)) return ALL_SIDES;

        int wet = 0;
        BlockPos.MutableBlockPos neighbour = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            neighbour.setWithOffset(pos, direction);
            if (level.getFluidState(neighbour).is(FluidTags.WATER)) wet++;
        }
        return wet;
    }

    /**
     * Replaces vanilla's oxidation roll for wet copper: advances one stage with chance wetSides * chancePerWetSide.
     * Returns whether it handled the roll; false (dry block) lets vanilla run unchanged
     */
    public static boolean tryOxidize(BlockState state, ServerLevel level, BlockPos pos, float roll) {
        int wet = wetSides(state, level, pos);
        if (wet == 0) return false;

        float chance = Math.min(1f, wet * Math.max(0f, ConfigIO.CURRENT.chancePerWetSide));
        if (roll < chance) {
            // Same setter as vanilla's changeOverTime, without its neighbour scan
            ((ChangeOverTimeBlock<?>) state.getBlock()).getNext(state).ifPresent(next -> level.setBlockAndUpdate(pos, next));
        }
        return true;
    }
}
