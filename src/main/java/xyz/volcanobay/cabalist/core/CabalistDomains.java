package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.spell.Domain;

import java.util.function.Function;

public class CabalistDomains {
    public static final ResourceKey<Registry<Domain>> DOMAIN_KEY = ResourceKey.createRegistryKey(Cabalist.id("domains"));

    private static final RegistrationProvider<Domain> DOMAIN = RegistrationProvider.get(DOMAIN_KEY, Cabalist.MODID);
    public static final Registry<Domain> DOMAIN_REGISTRY = DOMAIN.asVanillaRegistry();

    public static final RegistryObject<Domain> LIMIT = registerDomain("limit", Domain::new);

    private static RegistryObject<Domain> registerDomain(String name, Function<ResourceLocation, Domain> termFunction) {
        ResourceLocation location = Cabalist.id(name);
        Domain domain = termFunction.apply(location);
        return DOMAIN.register(location, () -> domain);
    }

    public static void bootstrap() {
    }
}
