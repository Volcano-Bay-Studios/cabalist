package xyz.volcanobay.cabalist.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;
import xyz.volcanobay.cabalist.core.CabalistBlockEntities;
import xyz.volcanobay.cabalist.core.CabalistBlocks;
import xyz.volcanobay.cabalist.core.CabalistTerms;
import xyz.volcanobay.cabalist.system.spell.SpellTriggers;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;

public class RuneBlock extends ContractHolderBlock {
    public static final MapCodec<RuneBlock> CODEC = simpleCodec(RuneBlock::new);
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 1, 16);

    public RuneBlock(Properties properties) {
        super(properties);
    }

    public static BlockPos getPlacePos(Level level, BlockPos pos) {
        return level.getBlockState(pos).isAir() ? pos : pos.above();
    }

    public static @Nullable ContractBlockEntity place(Level level, BlockPos runePos) {
        if (!level.getBlockState(runePos).canBeReplaced()) {
            return null;
        }
        level.setBlockAndUpdate(runePos, CabalistBlocks.RUNE.get().defaultBlockState());
        return level.getBlockEntity(runePos) instanceof ContractBlockEntity rune ? rune : null;
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<ContractBlockEntity> getBlockEntityType() {
        return CabalistBlockEntities.RUNE.get();
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ContractBlockEntity holder) {
            SpellTriggers.raise(CabalistTerms.STEPPED_ON.getId(), holder.getHost(), EntitySubject.of(entity));
        }
    }
}
