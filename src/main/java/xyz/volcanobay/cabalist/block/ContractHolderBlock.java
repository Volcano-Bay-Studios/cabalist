package xyz.volcanobay.cabalist.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;
import xyz.volcanobay.cabalist.core.CabalistTerms;
import xyz.volcanobay.cabalist.system.spell.SpellTriggers;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;

public abstract class ContractHolderBlock extends BaseEntityBlock {

    public ContractHolderBlock(Properties properties) {
        super(properties);
    }

    protected abstract BlockEntityType<ContractBlockEntity> getBlockEntityType();

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ContractBlockEntity(getBlockEntityType(), pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, getBlockEntityType(), ContractBlockEntity::serverTick);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ContractBlockEntity holder) {
            SpellTriggers.raise(CabalistTerms.TOUCHED.getId(), holder.getHost(), EntitySubject.of(player));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ContractBlockEntity holder) {
            SpellTriggers.raiseEverywhere(CabalistTerms.DIES.getId(), holder.getHost(), null);
            holder.destroyContract();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
