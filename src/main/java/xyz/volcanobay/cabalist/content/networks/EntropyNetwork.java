package xyz.volcanobay.cabalist.content.networks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import xyz.volcanobay.cabalist.core.CabalistBlocks;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistTags;
import xyz.volcanobay.cabalist.system.network.Network;
import xyz.volcanobay.cabalist.system.network.spatial.ManagedSpatialNetwork;

import java.util.Set;

public class EntropyNetwork extends Network {
    private int freeEntropyCapacity = 0;
    private int entropyCapacity = 0;

    public EntropyNetwork(Integer id) {
        super(id);
    }

    @Override
    public boolean shouldConnect(BlockState firstState, BlockState secondState, Level level, BlockPos first, BlockPos second) {
        return firstState.is(CabalistTags.ENTROPETIC) && secondState.is(CabalistTags.ENTROPETIC);
    }

    @Override
    protected boolean isMember(long x, long y, long z, Level level, ResourceLocation location) {
        return level.getBlockState(new BlockPos((int) x, (int) y, (int) z)).is(CabalistTags.ENTROPETIC);
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
                        entropyCapacity += CabalistConfig.ENTROPY_CAPACITY_PER_SUPERHEATED_SAND.get();
                    }
                }
            }
        }
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
