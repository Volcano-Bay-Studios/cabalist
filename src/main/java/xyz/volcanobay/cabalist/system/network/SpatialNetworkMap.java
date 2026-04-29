package xyz.volcanobay.cabalist.system.network;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistSpatialNetworks;
import xyz.volcanobay.cabalist.system.network.spatial.ManagedSpatialNetwork;

import java.util.*;
import java.util.function.Function;

public class SpatialNetworkMap<T extends Network> extends ManagedSpatialNetwork {
    private final Int2ObjectOpenHashMap<T> networks = new Int2ObjectOpenHashMap<>();
    private final ResourceLocation location;
    private final Function<Integer, T> factory;
    private final T defaultNetwork;
    private final Level level;

    private static final int MAX_SIZE = 10_000;

    public SpatialNetworkMap(Level level, ResourceLocation location, Function<Integer, T> function) {
        super(1000);
        this.location = location;
        this.factory = function;
        this.defaultNetwork = factory.apply(-1);
        this.level = level;
    }

    public T getNetwork(int id) {
        T network = networks.get(id);
        if (network == null) {
            network = factory.apply(id);
            network.setNetworkAccess(this);
            network.update();
            networks.put(id, network);
        }
        return network;
    }

    /**
     * Returns a network of blocks at a given oct coordinate.
     * This will try to find an existing network, otherwise it will create a new one.
     * @param x Long position of x
     * @param y Long position of y
     * @param z Long position of z
     * @return Either a network, or null if none can be found or created.
     */
    @Nullable
    public T getNetwork(long x, long y, long z) {
        int id = getOrDiscover(x, y, z);
        if (id == -1) {
            return null;
        }
        return getNetwork(id);
    }

    /**
     * Returns a network of blocks at a given oct coordinate.
     * This will try to find an existing network, otherwise it will create a new one.
     * @param pos Oct position in the world
     * @return Either a network, or null if none can be found or created.
     */
    @Nullable
    public T getNetwork(BlockPos pos) {
        return getNetwork(pos.getX(),pos.getY(),pos.getZ());
    }

    @Override
    protected boolean canConnect(long x1, long y1, long z1, long x2, long y2, long z2) {
        BlockPos first = new BlockPos((int) x1, (int) y1, (int) z1);
        BlockPos second = new BlockPos((int) x2, (int) y2, (int) z2);
        return defaultNetwork.shouldConnect(level.getBlockState(first),level.getBlockState(second),level,first,second);
    }

    @Override
    protected boolean isMember(long x, long y, long z) {
        BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
        BlockState blockState = level.getBlockState(pos);
        if (blockState.getBlock() instanceof ISpatialNetworkable networkable) {
            for (CabalistSpatialNetworks.NetworkHolder<? extends Network> networkHolder : networkable.spatialNetworks()) {
                if (networkHolder.getLocation().equals(location)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected Collection<long[]> discoverNetworkMembers(long x, long y, long z) {
        Deque<SmallOctPos> queue = new ArrayDeque<>();
        Set<SmallOctPos> visited = new HashSet<>();
        List<long[]> result = new ArrayList<>();

        SmallOctPos start = new SmallOctPos(x, y, z);
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            SmallOctPos current = queue.poll();
            result.add(new long[]{current.x(), current.y(), current.z()});

            for (int i = 0; i < 6; i++) {
                long nx = current.x() + (i == 0 ? 1 : i == 1 ? -1 : 0);
                long ny = current.y() + (i == 2 ? 1 : i == 3 ? -1 : 0);
                long nz = current.z() + (i == 4 ? 1 : i == 5 ? -1 : 0);

                SmallOctPos neighbor = new SmallOctPos(nx, ny, nz);

                if (visited.contains(neighbor)) continue;

                if (canConnect(current.x(), current.y(), current.z(), nx, ny, nz)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return result;
    }

    @Override
    public void update(int id) {
        getNetwork(id).update();
    }

    @Override
    public void merge(int first, int second) {
        getNetwork(first).update();
    }

    @Override
    public void split(int first, int second) {
        getNetwork(first).update();
        getNetwork(second).update();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(networkPositions.size());

        for (Map.Entry<Integer, Set<SmallOctPos>> entry : networkPositions.entrySet()) {
            int rootId = entry.getKey();
            Set<SmallOctPos> positions = entry.getValue();

            buf.writeVarInt(rootId);
            buf.writeVarInt(positions.size());

            for (SmallOctPos pos : positions) {
                buf.writeLong(pos.x());
                buf.writeLong(pos.y());
                buf.writeLong(pos.z());
            }
        }

        buf.writeVarInt(networks.size());
        networks.forEach((integer, t) -> {
            buf.writeVarInt(integer);
            t.write(buf);
        });
    }

    public void read(FriendlyByteBuf buf) {
        this.dsu.clear();
        this.networkPositions.clear();

         this.cache.clear();

        int highestId = 0;
        int networkCount = buf.readVarInt();

        for (int i = 0; i < networkCount; i++) {
            int rootId = buf.readVarInt();
            int posCount = buf.readVarInt();

            if (rootId > highestId) {
                highestId = rootId;
            }

            this.dsu.put(rootId, rootId);

            Set<SmallOctPos> positions = new HashSet<>(posCount);
            this.networkPositions.put(rootId, positions);

            for (int j = 0; j < posCount; j++) {
                long x = buf.readLong();
                long y = buf.readLong();
                long z = buf.readLong();

                SmallOctPos pos = new SmallOctPos(x, y, z);
                positions.add(pos);

                this.cache.put(x, y, z, rootId);
            }
        }

        this.idGenerator.set(highestId + 1);

        int networkObjectCount = buf.readVarInt();
        for (int i = 0; i < networkObjectCount; i++) {
            int key = buf.readVarInt();

            T network = factory.apply(key);
            network.setNetworkAccess(this);
            network.update();
            network.read(buf);
            networks.put(key, network);
        }
    }
}
