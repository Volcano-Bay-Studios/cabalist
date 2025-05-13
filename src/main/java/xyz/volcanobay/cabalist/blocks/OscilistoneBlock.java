package xyz.volcanobay.cabalist.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiConsumer;

public class OscilistoneBlock extends Block {
    public OscilistoneBlock(Properties blockProperties) {
        super(blockProperties);
    }

    @Override
    protected void onExplosionHit(BlockState p_311951_, Level p_312820_, BlockPos p_312489_, Explosion p_312925_, BiConsumer<ItemStack, BlockPos> p_312073_) {
    }
}
