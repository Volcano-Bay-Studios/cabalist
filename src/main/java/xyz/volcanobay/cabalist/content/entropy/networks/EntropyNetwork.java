package xyz.volcanobay.cabalist.content.entropy.networks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.core.CabalistBlocks;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.system.network.Network;
import xyz.volcanobay.cabalist.system.network.spatial.ManagedSpatialNetwork;
import xyz.volcanobay.cabalist.util.BlockHelper;

import java.util.Set;

public class EntropyNetwork extends Network {
    private double freeEntropyCapacity = 0;
    private double storedEntropyCapacity = 0;
    private double freeEntropy = 0;
    private double storedEntropy = 0;

    public EntropyNetwork(Integer id) {
        super(id);
    }

    @Override
    public boolean shouldConnect(BlockState firstState, BlockState secondState, Level level, BlockPos first, BlockPos second) {
        return BlockHelper.isEntropetic(firstState,first,level) && BlockHelper.isEntropetic(secondState,second,level);
    }

    @Override
    public boolean isMember(long x, long y, long z, Level level, ResourceLocation location) {
        BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
        BlockState blockState = level.getBlockState(pos);
        return BlockHelper.isEntropetic(blockState, pos, level);
    }

    public double getFreeEntropy() {
        return freeEntropy;
    }

    public double getStoredEntropy() {
        return storedEntropy;
    }

    public double addFreeEntropy(double amount) {
        double amountToAdd = Math.min(amount, freeEntropyCapacity - freeEntropy);
        freeEntropy += amountToAdd;
        return amountToAdd;
    }

    @Override
    public void update() {
        if (networkAccess != null) {
            Level level = networkAccess.getLevel();
            if (level != null) {
                freeEntropyCapacity = 0;
                Set<ManagedSpatialNetwork.SmallOctPos> networkMembers = networkAccess.getNetworkMembers(id);
                for (ManagedSpatialNetwork.SmallOctPos networkMember : networkMembers) {
                    freeEntropyCapacity += CabalistConfig.FREE_ENTROPY_PER_NETWORK_MEMBER.get();
                    BlockPos pos = new BlockPos((int) networkMember.x(), (int) networkMember.y(), (int) networkMember.z());
                    BlockState blockState = level.getBlockState(pos);
                    if (blockState.is(CabalistBlocks.SUPERHEATED_SAND.get())) {
                        storedEntropyCapacity += CabalistConfig.ENTROPY_CAPACITY_PER_SUPERHEATED_SAND.get();
                    }
                }
            }
        }
    }

    @Override
    public void tick(ServerLevel level) {
        super.tick(level);
        Cabalist.LOGGER.info("Entropy Network: " + freeEntropy + "/" + freeEntropyCapacity);
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
