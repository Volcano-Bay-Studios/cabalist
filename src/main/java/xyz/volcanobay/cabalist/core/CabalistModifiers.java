package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.content.contract.terms.LimitTerm;
import xyz.volcanobay.cabalist.system.spell.Modifier;

import java.util.function.Function;

public class CabalistModifiers {
    public static final ResourceKey<Registry<Modifier>> MODIFIER_KEY = ResourceKey.createRegistryKey(Cabalist.id("modifiers"));

    private static final RegistrationProvider<Modifier> MODIFIER = RegistrationProvider.get(MODIFIER_KEY, Cabalist.MODID);
    public static final Registry<Modifier> MODIFIER_REGISTRY = MODIFIER.asVanillaRegistry();

    public static final RegistryObject<Modifier> LIMIT = registerModifier("limit", Modifier::new);

    private static RegistryObject<Modifier> registerModifier(String name, Function<ResourceLocation, Modifier> termFunction) {
        ResourceLocation location = Cabalist.id(name);
        return MODIFIER.register(location, () -> termFunction.apply(location));
    }

    public static void bootstrap() {
    }
}
