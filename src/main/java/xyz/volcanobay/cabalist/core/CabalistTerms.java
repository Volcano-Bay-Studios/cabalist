package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.content.term.AfterPreviousTerm;
import xyz.volcanobay.cabalist.content.term.ConsentTerm;
import xyz.volcanobay.cabalist.content.term.LimitTerm;
import xyz.volcanobay.cabalist.content.term.RequirementTerm;
import xyz.volcanobay.cabalist.system.contract.Term;

import java.util.function.Function;

public class CabalistTerms {

    public static final ResourceKey<Registry<Term>> TERM_KEY = ResourceKey.createRegistryKey(Cabalist.id("terms"));

    private static final RegistrationProvider<Term> TERMS = RegistrationProvider.get(TERM_KEY, Cabalist.MODID);
    public static final Registry<Term> TERM_REGISTRY = TERMS.asVanillaRegistry();

    public static final RegistryObject<Term> LIMIT = registerTerm("limit", LimitTerm::new);
    public static final RegistryObject<Term> AFTER_PREVIOUS = registerTerm("after_previous", AfterPreviousTerm::new);
    public static final RegistryObject<Term> CONSENT = registerTerm("consent", ConsentTerm::new);

    public static final RegistryObject<Term> TOUCHED = registerTerm("touched", RequirementTerm::new);
    public static final RegistryObject<Term> STEPPED_ON = registerTerm("stepped_on", RequirementTerm::new);
    public static final RegistryObject<Term> STRUCK = registerTerm("struck", RequirementTerm::new);
    public static final RegistryObject<Term> STRIKES = registerTerm("strikes", RequirementTerm::new);
    public static final RegistryObject<Term> BLOODIED = registerTerm("bloodied", RequirementTerm::new);
    public static final RegistryObject<Term> DIES = registerTerm("dies", RequirementTerm::new);
    public static final RegistryObject<Term> RELEASED = registerTerm("released", RequirementTerm::new);

    private static RegistryObject<Term> registerTerm(String name, Function<ResourceLocation, Term> termFunction) {
        ResourceLocation location = Cabalist.id(name);
        return TERMS.register(location, () -> termFunction.apply(location));
    }

    public static void bootstrap() {
    }

}
