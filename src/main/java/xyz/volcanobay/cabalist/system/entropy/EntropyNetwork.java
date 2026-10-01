package xyz.volcanobay.cabalist.system.entropy;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import xyz.volcanobay.cabalist.core.CabalistBlocks;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.network.Network;
import xyz.volcanobay.cabalist.system.network.spatial.ManagedSpatialNetwork;
import xyz.volcanobay.cabalist.util.BlockHelper;

import java.util.Set;
import java.util.UUID;

public class EntropyNetwork extends Network {
    private static final double LOSS_SMOOTHING = 0.1;

    private double freeEntropyCapacity = 0;
    private double storedEntropyCapacity = 0;
    private double freeEntropy = 0;
    private double storedEntropy = 0;
    private int oscilistones;
    private int superheatedSand;
    private double lostThisSecond;
    private double averageLoss;

    private final BlockPos.MutableBlockPos memberPos = new BlockPos.MutableBlockPos();

    public EntropyNetwork(Integer id) {
        super(id);
    }

    @Override
    public boolean shouldConnect(BlockState firstState, BlockState secondState, Level level, BlockPos first, BlockPos second) {
        return BlockHelper.isEntropetic(firstState,first,level) && BlockHelper.isEntropetic(secondState,second,level);
    }

    @Override
    public boolean isMember(long x, long y, long z, Level level, ResourceLocation location) {
        memberPos.set((int) x, (int) y, (int) z);
        BlockState blockState = level.getBlockState(memberPos);
        return BlockHelper.isEntropetic(blockState, memberPos, level);
    }

    public double getFreeEntropy() {
        return freeEntropy;
    }

    public double getStoredEntropy() {
        return storedEntropy;
    }

    public double getFreeEntropyCapacity() {
        return freeEntropyCapacity;
    }

    public double getStoredEntropyCapacity() {
        return storedEntropyCapacity;
    }

    public double getProductionPerSecond() {
        return oscilistones * CabalistConfig.OSCILISTONE_ENTROPY_PER_SECOND.get();
    }

    public double getUpkeepPerSecond() {
        return oscilistones * CabalistConfig.OSCILISTONE_UPKEEP_PER_SECOND.get() + superheatedSand * CabalistConfig.SUPERHEATED_SAND_UPKEEP_PER_SECOND.get();
    }

    // Everything drawn out, by spells and by the network's own upkeep, averaged over the last several seconds.
    public double getAverageLossPerSecond() {
        return averageLoss;
    }

    public double addFreeEntropy(double amount) {
        double amountToAdd = Math.min(amount, freeEntropyCapacity - freeEntropy);
        freeEntropy += amountToAdd;
        return amountToAdd;
    }

    public double extractFreeEntropy(double max) {
        double extracted = Math.min(freeEntropy, Math.max(max, 0));
        freeEntropy -= extracted;
        lostThisSecond += extracted;
        return extracted;
    }

    @Override
    public void onAbsorbed(Network absorbed) {
        if (absorbed instanceof EntropyNetwork other && other != this) {
            freeEntropy += other.freeEntropy;
            storedEntropy += other.storedEntropy;
            other.freeEntropy = 0;
            other.storedEntropy = 0;
        }
        Level level = networkAccess == null ? null : networkAccess.getLevel();
        if (level == null) {
            return;
        }
        Contract absorbedContract = ContractSystem.INSTANCE.getNetworkContract(level.dimension(), absorbed.getId());
        if (absorbedContract == null) {
            return;
        }
        Contract contract = EntropyNetworkContract.getOrCreate(level, id);
        for (UUID memberId : absorbedContract.getMemberIds()) {
            contract.addMemberId(memberId);
        }
        ContractSystem.INSTANCE.removeContract(absorbedContract);
    }

    @Override
    public void onSplitInto(Network created) {
        Level level = networkAccess == null ? null : networkAccess.getLevel();
        if (level == null) {
            return;
        }
        Contract contract = ContractSystem.INSTANCE.getNetworkContract(level.dimension(), id);
        if (contract == null) {
            return;
        }
        Contract createdContract = EntropyNetworkContract.getOrCreate(level, created.getId());
        for (UUID memberId : contract.getMemberIds()) {
            createdContract.addMemberId(memberId);
        }
    }

    @Override
    public void update() {
        if (networkAccess != null) {
            Level level = networkAccess.getLevel();
            if (level != null) {
                freeEntropyCapacity = 0;
                storedEntropyCapacity = 0;
                oscilistones = 0;
                superheatedSand = 0;
                Set<ManagedSpatialNetwork.SmallOctPos> networkMembers = networkAccess.getNetworkMembers(id);
                for (ManagedSpatialNetwork.SmallOctPos networkMember : networkMembers) {
                    freeEntropyCapacity += CabalistConfig.FREE_ENTROPY_PER_NETWORK_MEMBER.get();
                    memberPos.set((int) networkMember.x(), (int) networkMember.y(), (int) networkMember.z());
                    BlockState blockState = level.getBlockState(memberPos);
                    if (blockState.is(CabalistBlocks.SUPERHEATED_SAND.get())) {
                        storedEntropyCapacity += CabalistConfig.ENTROPY_CAPACITY_PER_SUPERHEATED_SAND.get();
                        superheatedSand++;
                    } else if (blockState.is(CabalistBlocks.OSCILISTONE.get())) {
                        oscilistones++;
                    }
                }
            }
        }
    }

    // Oscilistone makes entropy into the free pool, and whatever overflows is kept in superheated sand.
    // Spells only ever see the free pool, so the sand refills it as it's drawn down.
    @Override
    public void tick(ServerLevel level) {
        if (networkAccess == null || !networkAccess.isRoot(id)) {
            return;
        }
        freeEntropy += getProductionPerSecond() / 20;
        double upkeep = getUpkeepPerSecond() / 20;
        double fromFree = Math.min(freeEntropy, upkeep);
        freeEntropy -= fromFree;
        storedEntropy = Math.max(0, storedEntropy - (upkeep - fromFree));
        lostThisSecond += upkeep;
        if (freeEntropy > freeEntropyCapacity) {
            storedEntropy = Math.min(storedEntropyCapacity, storedEntropy + freeEntropy - freeEntropyCapacity);
            freeEntropy = freeEntropyCapacity;
        } else {
            double refill = Math.min(storedEntropy, freeEntropyCapacity - freeEntropy);
            storedEntropy -= refill;
            freeEntropy += refill;
        }
        if (level.getGameTime() % 20 == 0) {
            averageLoss += (lostThisSecond - averageLoss) * LOSS_SMOOTHING;
            lostThisSecond = 0;
        }
    }

    @Override
    public CompoundTag saveState() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("free", freeEntropy);
        tag.putDouble("stored", storedEntropy);
        return tag;
    }

    @Override
    public void loadState(CompoundTag tag) {
        freeEntropy = tag.getDouble("free");
        storedEntropy = tag.getDouble("stored");
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        super.write(buf);
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        super.read(buf);
    }
}
