package xyz.volcanobay.cabalist.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import xyz.volcanobay.cabalist.core.CabalistTags;
import xyz.volcanobay.cabalist.system.entropy.Entropetic;

public class BlockHelper {
    public static boolean isEntropetic(BlockState state, BlockPos pos, Level level) {
        return state.is(CabalistTags.ENTROPETIC) || state.getBlock() instanceof Entropetic;
    }

    public static int placeFireOn(Level level, BlockPos pos) {
        int amountLit = 0;
        for (Direction direction : Direction.values()) {
            BlockPos relative = pos.relative(direction);
            if (level.getBlockState(relative).isAir() && BaseFireBlock.canBePlacedAt(level, relative, direction)) {
                level.setBlockAndUpdate(relative, BaseFireBlock.getState(level, relative));
                amountLit++;
            }
        }
        return amountLit;
    }
}
