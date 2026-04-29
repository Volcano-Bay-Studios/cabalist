package xyz.volcanobay.cabalist.system.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.core.CabalistSpatialNetworks;
import xyz.volcanobay.cabalist.core.CabalistTags;

@EventBusSubscriber(modid = Cabalist.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkEventListener {
    @SubscribeEvent
    public static void blockBreak(BlockEvent.BreakEvent event) {
        LevelAccessor level = event.getLevel();
        BlockPos pos = event.getPos();
        if (level.getBlockState(pos).is(CabalistTags.ENTROPETIC)) {
            CabalistSpatialNetworks.ENTROPY_NETWORK.get((Level) level).removePoint(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    @SubscribeEvent
    public static void blockPlace(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor level = event.getLevel();
        BlockPos pos = event.getPos();
        if (level.getBlockState(pos).is(CabalistTags.ENTROPETIC)) {
            CabalistSpatialNetworks.ENTROPY_NETWORK.get((Level) level).addPoint(pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
