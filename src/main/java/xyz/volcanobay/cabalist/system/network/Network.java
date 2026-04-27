package xyz.volcanobay.cabalist.system.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistSpatialNetworks;

public abstract class Network {
    protected final int id;

    public Network(Integer id) {
        this.id = id;
    }

    @Nullable
    public ISpatialNetworkable getBlockOverride(BlockState blockState, Level level, BlockPos pos) {
        return blockState.getBlock() instanceof ISpatialNetworkable iSpatialNetworkable ? iSpatialNetworkable : null;
    }

    public boolean shouldConnect(BlockState firstState, BlockState secondState, Level level, BlockPos first, BlockPos second) {
        if (getBlockOverride(firstState,level,first) instanceof ISpatialNetworkable firstNetworkable && getBlockOverride(secondState,level,second) instanceof ISpatialNetworkable secondNetworkable) {
            if (firstNetworkable.shouldConnectNetwork(firstState, secondState, level, first, second) && secondNetworkable.shouldConnectNetwork(secondState,firstState,level,second,first)) {
                for (CabalistSpatialNetworks.NetworkHolder<? extends Network> spatialNetwork : firstNetworkable.spatialNetworks()) {
                    if (secondNetworkable.spatialNetworks().contains(spatialNetwork)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
