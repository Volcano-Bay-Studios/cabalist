package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.energy.EnergyType;

public class CabalistEnergyTypes {
    public static final ResourceKey<Registry<EnergyType>> ENERGY_TYPE_KEY = ResourceKey.createRegistryKey(Cabalist.id("energy_type"));

    private static final RegistrationProvider<EnergyType> ENERGY_TYPES = RegistrationProvider.get(ENERGY_TYPE_KEY, Cabalist.MODID);
    public static final Registry<EnergyType> ENERGY_TYPE_REGISTRY = ENERGY_TYPES.asVanillaRegistry();

    public static final RegistryObject<EnergyType> ENTROPY = ENERGY_TYPES.register("entropy", EnergyType::new);
    public static final RegistryObject<EnergyType> LIFEFORCE = ENERGY_TYPES.register("lifeforce", EnergyType::new);

    public static void bootstrap() {
    }
}
