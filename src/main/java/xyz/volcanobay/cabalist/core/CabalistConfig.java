package xyz.volcanobay.cabalist.core;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import xyz.volcanobay.cabalist.Cabalist;

@EventBusSubscriber(modid = Cabalist.MODID)
public class CabalistConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<Integer> FREE_ENTROPY_PER_NETWORK_MEMBER = BUILDER.defineInRange("free_entropy_per_network_member", 10, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.ConfigValue<Integer> ENTROPY_CAPACITY_PER_SUPERHEATED_SAND = BUILDER.defineInRange("entropy_capacity_per_superheated_sand", 500, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue OSCILISTONE_ENTROPY_PER_SECOND = BUILDER.defineInRange("oscilistone_entropy_per_second", 0.3, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue OSCILISTONE_UPKEEP_PER_SECOND = BUILDER.defineInRange("oscilistone_upkeep_per_second", 0.0333, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue SUPERHEATED_SAND_UPKEEP_PER_SECOND = BUILDER.defineInRange("superheated_sand_upkeep_per_second", 0.005, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue GATHER_RATE_PER_SECOND = BUILDER.defineInRange("gather_rate_per_second", 30.0, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue LIFEFORCE_GATHER_SPEEDUP = BUILDER.defineInRange("lifeforce_gather_speedup", 1.0, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue LIFEFORCE_GATHER_SCALE = BUILDER.defineInRange("lifeforce_gather_scale", 5000.0, 1, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue GATHER_SLOWDOWN_SECONDS = BUILDER.defineInRange("gather_slowdown_seconds", 20.0, 0.01, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue CAPACITY_PER_CHAR = BUILDER.defineInRange("capacity_per_char", 4.0, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue CAPACITY_EXPONENT = BUILDER.defineInRange("capacity_exponent", 1.4, 1, 4);
    public static final ModConfigSpec.DoubleValue DRAW_SAFE_THROUGHPUT = BUILDER.defineInRange("draw_safe_throughput", 2.0, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue STANDARD_CHARGE_SECONDS = BUILDER.defineInRange("standard_charge_seconds", 0.8, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue GLYPH_DROP_MIN_FRACTION = BUILDER.defineInRange("glyph_drop_min_fraction", 0.03, 0, 1);
    public static final ModConfigSpec.DoubleValue GLYPH_DROP_MAX_FRACTION = BUILDER.defineInRange("glyph_drop_max_fraction", 0.08, 0, 1);
    public static final ModConfigSpec.DoubleValue CONTRACT_RECHARGE_SECONDS = BUILDER.defineInRange("contract_recharge_seconds", 60.0, 0.01, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue LIFEFORCE_RECOVERY_SECONDS = BUILDER.defineInRange("lifeforce_recovery_seconds", 60.0, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue ENTROPY_PER_HEALTH = BUILDER.defineInRange("entropy_per_health", 40.0, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue SECONDS_PER_SPOKEN_WORD = BUILDER.defineInRange("seconds_per_spoken_word", 0.4, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue RIFT_COST_PER_BLOCK = BUILDER.defineInRange("rift_cost_per_block", 0.05, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue CROSS_DIMENSION_DISTANCE = BUILDER.defineInRange("cross_dimension_distance", 10000.0, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.IntValue REQUIREMENT_WINDOW_TICKS = BUILDER.defineInRange("requirement_window_ticks", 100, 1, Integer.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue FORM_AMBIENT_FRACTION_PER_SECOND = BUILDER.defineInRange("form_ambient_fraction_per_second", 0.05, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue HANGING_UPKEEP_PER_SECOND = BUILDER.defineInRange("hanging_upkeep_per_second", 0.5, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue DEVOTION_GENERIC_RATE = BUILDER.defineInRange("devotion_generic_rate", 0.25, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue DEVOTION_PHRASE_RATE = BUILDER.defineInRange("devotion_phrase_rate", 0.4, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue DEVOTION_DOMAIN_WORD_RATE = BUILDER.defineInRange("devotion_domain_word_rate", 0.5, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue DEVOTION_REPEAT_FALLOFF = BUILDER.defineInRange("devotion_repeat_falloff", 0.25, 0, 1);
    public static final ModConfigSpec.DoubleValue DEVOTION_STRENGTH_RANGE = BUILDER.defineInRange("devotion_strength_range", 0.5, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue DEVOTION_COST_RANGE = BUILDER.defineInRange("devotion_cost_range", 0.5, 0, 0.95);
    public static final ModConfigSpec.DoubleValue DEVOTION_SIZE_RANGE = BUILDER.defineInRange("devotion_size_range", 0.15, 0, Double.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue DEVOTION_CAPACITY_RANGE = BUILDER.defineInRange("devotion_capacity_range", 1.0, 0, Double.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue VOICE_ENABLED = BUILDER.define("voice_enabled",true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
    }
}
