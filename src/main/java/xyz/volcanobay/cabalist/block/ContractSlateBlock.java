package xyz.volcanobay.cabalist.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;
import xyz.volcanobay.cabalist.core.CabalistBlockEntities;
import xyz.volcanobay.cabalist.core.CabalistTerms;
import xyz.volcanobay.cabalist.system.spell.SpellTriggers;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;

public class ContractSlateBlock extends ContractHolderBlock {
    public static final MapCodec<ContractSlateBlock> CODEC = simpleCodec(ContractSlateBlock::new);

    public ContractSlateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<ContractBlockEntity> getBlockEntityType() {
        return CabalistBlockEntities.CONTRACT_SLATE.get();
    }

    @Override
    public void stepOn(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Entity entity) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ContractBlockEntity holder) {
            SpellTriggers.raise(CabalistTerms.STEPPED_ON.getId(), holder.getHost(), EntitySubject.of(entity));
        }
        super.stepOn(level, pos, state, entity);
    }
}
