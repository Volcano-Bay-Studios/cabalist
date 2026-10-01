package xyz.volcanobay.cabalist.system.focus;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Source of a spells energy by mob
 */
public record SpellSource(Map<ResourceLocation, Double> imbued) {

    public static final SpellSource NONE = new SpellSource(Map.of());

    public double getTotal() {
        double total = 0;
        for (double amount : imbued.values()) {
            total += amount;
        }
        return total;
    }
}
