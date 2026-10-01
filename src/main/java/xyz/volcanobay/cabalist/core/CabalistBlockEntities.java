package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;

import java.util.function.Supplier;

public class CabalistBlockEntities {

    public static final RegistrationProvider<BlockEntityType<?>> BLOCK_ENTITIES = RegistrationProvider.get(Registries.BLOCK_ENTITY_TYPE, Cabalist.MODID);

    public static final RegistryObject<BlockEntityType<ContractBlockEntity>> RUNE = registerBlockEntity("rune",
            () -> BlockEntityType.Builder.of((pos, state) -> new ContractBlockEntity(CabalistBlockEntities.RUNE.get(), pos, state), CabalistBlocks.RUNE.get()));

    public static final RegistryObject<BlockEntityType<ContractBlockEntity>> CONTRACT_SLATE = registerBlockEntity("contract_slate",
            () -> BlockEntityType.Builder.of((pos, state) -> new ContractBlockEntity(CabalistBlockEntities.CONTRACT_SLATE.get(), pos, state), CabalistBlocks.CONTRACT_SLATE.get()));

    private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> registerBlockEntity(String name, Supplier<BlockEntityType.Builder<T>> blockEntity) {
        return BLOCK_ENTITIES.register(name, () -> blockEntity.get().build(null));
    }

    public static void bootstrap() {
    }
}
