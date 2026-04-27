package xyz.volcanobay.cabalist.content.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import xyz.volcanobay.cabalist.core.CabalistSpatialNetworks;
import xyz.volcanobay.cabalist.system.network.ISpatialNetworkable;
import xyz.volcanobay.cabalist.system.network.Network;

import java.util.List;
import java.util.function.BiConsumer;

public class OscilistoneBlock extends Block implements ISpatialNetworkable {
    public OscilistoneBlock(Properties blockProperties) {
        super(blockProperties);
    }

    @Override
    protected void onExplosionHit(BlockState p_311951_, Level p_312820_, BlockPos p_312489_, Explosion p_312925_, BiConsumer<ItemStack, BlockPos> p_312073_) {
    }

    @Override
    public List<CabalistSpatialNetworks.NetworkHolder<? extends Network>> spatialNetworks() {
        return List.of(CabalistSpatialNetworks.ENTROPY_NETWORK);
    }

    @Override
    public boolean shouldConnectNetwork(BlockState firstState, BlockState secondState, Level level, BlockPos first, BlockPos second) {
        return first.distManhattan(second) < 2;
    }
}
