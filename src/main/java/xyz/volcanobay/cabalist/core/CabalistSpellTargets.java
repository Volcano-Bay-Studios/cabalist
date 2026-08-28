package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.spell.SpellTarget;

import java.util.function.Function;

public class CabalistSpellTargets {
    public static final ResourceKey<Registry<SpellTarget>> SPELL_TARGET_KEY = ResourceKey.createRegistryKey(Cabalist.id("spellTargets"));

    private static final RegistrationProvider<SpellTarget> SPELL_TARGET = RegistrationProvider.get(SPELL_TARGET_KEY, Cabalist.MODID);
    public static final Registry<SpellTarget> SPELL_TARGET_REGISTRY = SPELL_TARGET.asVanillaRegistry();

    public static final RegistryObject<SpellTarget> SELF = registerSpellTarget("self", SpellTarget::new);

    private static RegistryObject<SpellTarget> registerSpellTarget(String name, Function<ResourceLocation, SpellTarget> termFunction) {
        ResourceLocation location = Cabalist.id(name);
        SpellTarget spellTarget = termFunction.apply(location);
        return SPELL_TARGET.register(location, () -> spellTarget);
    }

    public static void bootstrap() {
    }
}
