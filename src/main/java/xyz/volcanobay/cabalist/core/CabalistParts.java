package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;

import java.util.function.Supplier;

public class CabalistParts {
    public static final ResourceKey<Registry<SpellComponent>> PART_KEY = ResourceKey.createRegistryKey(Cabalist.id("part"));

    private static final RegistrationProvider<SpellComponent> PART = RegistrationProvider.get(PART_KEY, Cabalist.MODID);
    public static final Registry<SpellComponent> PART_REGISTRY = PART.asVanillaRegistry();

    public static final RegistryObject<SpellComponent> FLAME = registerPart("flame", Domain::new);

    private static RegistryObject<SpellComponent> registerPart(String name, Supplier<SpellComponent> termFunction) {
        ResourceLocation location = Cabalist.id(name);
        return PART.register(location, termFunction);
    }

    public static void bootstrap() {
    }
}
