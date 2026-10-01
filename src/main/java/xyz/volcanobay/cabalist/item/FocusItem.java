package xyz.volcanobay.cabalist.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import xyz.volcanobay.cabalist.system.focus.Imbuement;
import xyz.volcanobay.cabalist.system.focus.LifeforceCaps;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class FocusItem extends Item {
    public static final Supplier<Imbuement> STANDARD = () -> atCap(Map.of(
            ResourceLocation.withDefaultNamespace("zombie"), 1.0,
            ResourceLocation.withDefaultNamespace("skeleton"), 1.0,
            ResourceLocation.withDefaultNamespace("creeper"), 1.0,
            ResourceLocation.withDefaultNamespace("spider"), 1.0,
            ResourceLocation.withDefaultNamespace("cow"), 1.0,
            ResourceLocation.withDefaultNamespace("pig"), 1.0));

    public static final Supplier<Imbuement> CREATIVE = () -> {
        Map<ResourceLocation, Double> everything = new HashMap<>();
        for (ResourceLocation type : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            everything.put(type, 1.0);
        }
        return atCap(everything);
    };

    private final Supplier<Imbuement> imbued;
    private Imbuement cached;

    public FocusItem(Properties properties, Supplier<Imbuement> imbued) {
        super(properties);
        this.imbued = imbued;
    }

    public Imbuement getImbued() {
        if (cached == null) {
            cached = imbued.get();
        }
        return cached;
    }

    private static Imbuement atCap(Map<ResourceLocation, Double> fractions) {
        Map<ResourceLocation, Double> lifeforce = new HashMap<>();
        fractions.forEach((type, fraction) -> {
            double amount = LifeforceCaps.get(type) * fraction;
            if (amount > 0) {
                lifeforce.put(type, amount);
            }
        });
        return new Imbuement(lifeforce, Map.of(), false, 0);
    }
}
