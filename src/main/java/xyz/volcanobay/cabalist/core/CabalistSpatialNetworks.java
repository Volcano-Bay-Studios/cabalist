package xyz.volcanobay.cabalist.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.content.networks.EntropyNetwork;
import xyz.volcanobay.cabalist.system.network.Network;
import xyz.volcanobay.cabalist.system.network.SpatialNetworkMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class CabalistSpatialNetworks {
    public static final List<NetworkHolder<? extends Network>> NETWORKS = new ArrayList<>();

    public static final NetworkHolder<EntropyNetwork> ENTROPY_NETWORK = registerNetwork("entropy_network", EntropyNetwork::new);

    public static <T extends Network> NetworkHolder<T> registerNetwork(String location, Function<Integer, T> function) {
        return registerNetwork(Cabalist.id(location), function);
    }

    public static <T extends Network> NetworkHolder<T> registerNetwork(ResourceLocation location, Function<Integer, T> function) {
        NetworkHolder<T> networkHolder = new NetworkHolder<>(location, function);
        NETWORKS.add(networkHolder);
        return networkHolder;
    }

    @ApiStatus.Internal
    public static void bootstrap() {
    }

    public static class NetworkHolder<T extends Network> {
        private final HashMap<ResourceKey<Level>, SpatialNetworkMap<T>> worldNetworkMap = new HashMap<>();
        private final ResourceLocation location;
        private final Function<Integer, T> function;

        public NetworkHolder(ResourceLocation location, Function<Integer, T> function) {
            this.location = location;
            this.function = function;
        }

        public ResourceLocation getLocation() {
            return location;
        }

        public SpatialNetworkMap<T> get(Level level) {
            ResourceKey<Level> dimension = level.dimension();
            return worldNetworkMap.computeIfAbsent(dimension, (levelResourceKey -> new SpatialNetworkMap<>(level, location, function)));
        }
    }
}
