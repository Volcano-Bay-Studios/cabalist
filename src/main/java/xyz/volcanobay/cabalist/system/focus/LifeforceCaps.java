package xyz.volcanobay.cabalist.system.focus;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import xyz.volcanobay.cabalist.core.CabalistConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The max lifeforce of a mob
 */
public final class LifeforceCaps {
    private static final Map<ResourceLocation, Double> CAPS = new ConcurrentHashMap<>();

    public static double get(ResourceLocation type) {
        return CAPS.computeIfAbsent(type, LifeforceCaps::getMaxHealth) * CabalistConfig.ENTROPY_PER_HEALTH.get();
    }

    public static ResourceLocation getType(LivingEntity living) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
    }

    @SuppressWarnings("unchecked")
    private static double getMaxHealth(ResourceLocation id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) {
            return 0;
        }
        EntityType<? extends LivingEntity> living = (EntityType<? extends LivingEntity>) type;
        if (!DefaultAttributes.hasSupplier(living)) {
            return 0;
        }
        return DefaultAttributes.getSupplier(living).getBaseValue(Attributes.MAX_HEALTH);
    }
}
