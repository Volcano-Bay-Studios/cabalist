package xyz.volcanobay.cabalist.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.subject.RuneSubject;

import java.util.UUID;

public class ContractBlockEntity extends BlockEntity {
    private @Nullable UUID contractId;
    private @Nullable RuneSubject host;

    public ContractBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ContractBlockEntity blockEntity) {
        Contract contract = blockEntity.getContract();
        if (contract != null) {
            contract.bindHost(blockEntity.getHost());
        }
    }

    public static @Nullable Contract getContractAt(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ContractBlockEntity holder ? holder.getContract() : null;
    }

    public RuneSubject getHost() {
        if (host == null) {
            host = new RuneSubject(level, worldPosition);
        }
        return host;
    }

    public @Nullable Contract getContract() {
        if (contractId == null) {
            return null;
        }
        return ContractSystem.INSTANCE.getContract(contractId);
    }

    public void setContract(Contract contract) {
        Contract previous = getContract();
        if (previous != null && previous != contract) {
            ContractSystem.INSTANCE.removeContract(previous);
        }
        contractId = contract.getUUID();
        setChanged();
    }

    // Lets go of the contract without destroying it, when it moves elsewhere.
    public void releaseContract() {
        Contract contract = getContract();
        if (contract != null && level != null) {
            contract.unbindHost(getHost());
        }
        contractId = null;
        setChanged();
    }

    public void destroyContract() {
        Contract contract = getContract();
        if (contract != null) {
            contract.logAction(null, "destroy", "", "accepted");
            ContractSystem.INSTANCE.removeContract(contract);
        }
        contractId = null;
    }

    @Override
    public void setRemoved() {
        Contract contract = getContract();
        if (contract != null && level != null && !level.isClientSide) {
            contract.unbindHost(getHost());
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (contractId != null) {
            tag.putUUID("contract", contractId);
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        contractId = tag.hasUUID("contract") ? tag.getUUID("contract") : null;
    }
}
