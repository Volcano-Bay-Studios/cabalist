package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.content.contract.terms.LimitTerm;
import xyz.volcanobay.cabalist.system.contract.Term;

import java.util.function.Function;

public class CabalistTerms {

    public static final ResourceKey<Registry<Term>> TERM_KEY = ResourceKey.createRegistryKey(Cabalist.id("terms"));

    private static final RegistrationProvider<Term> TERMS = RegistrationProvider.get(TERM_KEY, Cabalist.MODID);
    public static final Registry<Term> TERM_REGISTRY = TERMS.asVanillaRegistry();

    public static final RegistryObject<Term> LIMIT = registerTerm("limit", LimitTerm::new);

    private static RegistryObject<Term> registerTerm(String name, Function<ResourceLocation, Term> termFunction) {
        ResourceLocation location = Cabalist.id(name);
        return TERMS.register(location, () -> termFunction.apply(location));
    }

    public static void bootstrap() {
    }


}
