package xyz.volcanobay.cabalist.persistent;

import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import xyz.volcanobay.cabalist.core.CabalistSpatialNetworks;
import xyz.volcanobay.cabalist.system.network.Network;
import xyz.volcanobay.cabalist.system.network.SpatialNetworkMap;

public class NetworkSavedData extends SavedData {

    public static final String ID = "spatial_networks";
    private static final String STATE_SUFFIX = "/state";

    private ServerLevel level = null;

    public NetworkSavedData() {
        super();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { // This is cursed
        for (CabalistSpatialNetworks.NetworkHolder<? extends Network> spatialNetwork : CabalistSpatialNetworks.NETWORKS) {
            SpatialNetworkMap<? extends Network> spatialNetworkMap = spatialNetwork.get(level);
            if (spatialNetworkMap != null) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                spatialNetworkMap.write(buf);
                byte[] bytes = new byte[buf.writerIndex()];
                buf.readBytes(bytes);
                tag.putByteArray(spatialNetwork.getLocation().toString(), bytes);
                tag.put(spatialNetwork.getLocation() + STATE_SUFFIX, spatialNetworkMap.writeStates());
            }
        }
        return tag;
    }

    public static NetworkSavedData load(CompoundTag tag, ServerLevel level) {
        for (CabalistSpatialNetworks.NetworkHolder<? extends Network> network : CabalistSpatialNetworks.NETWORKS) {
            SpatialNetworkMap<? extends Network> spatialNetworkMap = network.get(level);
            if (spatialNetworkMap != null) {
                String location = network.getLocation().toString();
                if (tag.contains(location)) {
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(tag.getByteArray(location)));
                    spatialNetworkMap.read(buf);
                    spatialNetworkMap.readStates(tag.getCompound(location + STATE_SUFFIX));
                }
            }
        }
        return new NetworkSavedData();
    }

    public static NetworkSavedData get(ServerLevel level) {
        NetworkSavedData data = level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(NetworkSavedData::new,
                        (c, f) -> load(c, level),
                        DataFixTypes.LEVEL),
                ID);

        data.level = level;
        data.setDirty(true); //TODO: only make cable data dirty if cables have changed
        return data;
    }
}
