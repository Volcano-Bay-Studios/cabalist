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

public class CabalistSpellComponents {
    public static final ResourceKey<Registry<SpellComponent>> COMPONENT_KEY = ResourceKey.createRegistryKey(Cabalist.id("component"));

    private static final RegistrationProvider<SpellComponent> COMPONENT = RegistrationProvider.get(COMPONENT_KEY, Cabalist.MODID);
    public static final Registry<SpellComponent> PART_REGISTRY = COMPONENT.asVanillaRegistry();

    public static final RegistryObject<SpellComponent> ENTROPY = registerPart("entropy", Domain::new);
    public static final RegistryObject<SpellComponent> WATER = registerPart("water", Domain::new);

    private static RegistryObject<SpellComponent> registerPart(String name, Supplier<SpellComponent> termFunction) {
        ResourceLocation location = Cabalist.id(name);
        return COMPONENT.register(location, termFunction);
    }

    public static void bootstrap() {
    }
}
