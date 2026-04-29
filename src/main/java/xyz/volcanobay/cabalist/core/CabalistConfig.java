package xyz.volcanobay.cabalist.core;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import xyz.volcanobay.cabalist.Cabalist;

@EventBusSubscriber(modid = Cabalist.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CabalistConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<Integer> FREE_ENTROPY_PER_NETWORK_MEMBER = BUILDER.defineInRange("free_entropy_per_network_member", 10, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.ConfigValue<Integer> ENTROPY_CAPACITY_PER_SUPERHEATED_SAND = BUILDER.defineInRange("entropy_capacity_per_superheated_sand", 500, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec SPEC = BUILDER.build();


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
    }
}
