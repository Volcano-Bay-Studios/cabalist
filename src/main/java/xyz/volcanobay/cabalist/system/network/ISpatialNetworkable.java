package xyz.volcanobay.cabalist.system.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import xyz.volcanobay.cabalist.core.CabalistSpatialNetworks;

import java.util.List;

public interface ISpatialNetworkable {
    List<CabalistSpatialNetworks.NetworkHolder<? extends Network>> spatialNetworks();

    /**
     * Tells a spatial network if the first (oct)block allows connection to the second block.
     */
    default boolean shouldConnectNetwork(BlockState firstState, BlockState secondState, Level level, BlockPos first, BlockPos second) {
        return true;
    }


    default void place(BlockPos pos, Level level) {
        for (CabalistSpatialNetworks.NetworkHolder<? extends Network> spatialNetwork : spatialNetworks()) {
            spatialNetwork.get(level).addPoint(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    default void destroy(BlockPos pos, Level level) {
        for (CabalistSpatialNetworks.NetworkHolder<? extends Network> spatialNetwork : spatialNetworks()) {
            spatialNetwork.get(level).removePoint(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    default void update(BlockPos pos, Level level) {
        for (CabalistSpatialNetworks.NetworkHolder<? extends Network> spatialNetwork : spatialNetworks()) {
            spatialNetwork.get(level).updatePoint(pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
