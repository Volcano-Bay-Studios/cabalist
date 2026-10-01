package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.blood.BloodStain;
import xyz.volcanobay.cabalist.system.contract.Inscription;
import xyz.volcanobay.cabalist.system.focus.Imbuement;

import java.util.UUID;

public class CabalistDataComponents {
    private static final RegistrationProvider<DataComponentType<?>> DATA_COMPONENTS = RegistrationProvider.get(Registries.DATA_COMPONENT_TYPE, Cabalist.MODID);

    public static final RegistryObject<DataComponentType<UUID>> CONTRACT = DATA_COMPONENTS.register("contract",
            () -> DataComponentType.<UUID>builder().persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());

    public static final RegistryObject<DataComponentType<BloodStain>> BLOOD = DATA_COMPONENTS.register("blood",
            () -> DataComponentType.<BloodStain>builder().persistent(BloodStain.CODEC).networkSynchronized(BloodStain.STREAM_CODEC).build());

    public static final RegistryObject<DataComponentType<Imbuement>> IMBUEMENT = DATA_COMPONENTS.register("imbuement",
            () -> DataComponentType.<Imbuement>builder().persistent(Imbuement.CODEC).networkSynchronized(Imbuement.STREAM_CODEC).build());

    public static final RegistryObject<DataComponentType<Inscription>> INSCRIPTION = DATA_COMPONENTS.register("inscription",
            () -> DataComponentType.<Inscription>builder().persistent(Inscription.CODEC).networkSynchronized(Inscription.STREAM_CODEC).build());

    public static void bootstrap() {
    }
}
