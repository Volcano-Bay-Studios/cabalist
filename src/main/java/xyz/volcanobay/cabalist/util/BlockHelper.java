package xyz.volcanobay.cabalist.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import xyz.volcanobay.cabalist.content.entropy.networks.Entropetic;
import xyz.volcanobay.cabalist.core.CabalistTags;

public class BlockHelper {
    public static boolean isEntropetic(BlockState state, BlockPos pos, Level level) {
        return state.is(CabalistTags.ENTROPETIC) || state.getBlock() instanceof Entropetic;
    }
}
