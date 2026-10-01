package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.block.ContractSlateBlock;
import xyz.volcanobay.cabalist.block.OscilistoneBlock;
import xyz.volcanobay.cabalist.block.RuneBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class CabalistBlocks {
    public static final RegistrationProvider<Block> BLOCKS = RegistrationProvider.get(Registries.BLOCK, Cabalist.MODID);
    public static final List<RegistryObject<? extends Block>> ENTROPETIC_BLOCKS = new ArrayList<>();

    public static final RegistryObject<OscilistoneBlock> OSCILISTONE = registerEntropeticBlock("oscilistone",
            () -> new OscilistoneBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .strength(2.0f, 1f).noOcclusion()
            ));

    public static final RegistryObject<OscilistoneBlock> SUPERHEATED_SAND = registerEntropeticBlock("superheated_sand",
            () -> new OscilistoneBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)
                    .strength(2.0f, 1f).noOcclusion()
            ));

    public static final RegistryObject<RuneBlock> RUNE = BLOCKS.register("rune",
            () -> new RuneBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().replaceable()));

    public static final RegistryObject<ContractSlateBlock> CONTRACT_SLATE = registerBlock("contract_slate",
            () -> new ContractSlateBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).noOcclusion()));

    private static <T extends Block> RegistryObject<T> registerEntropeticBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        CabalistItems.registerItem(name, () -> new BlockItem(toReturn.get(), new Item.Properties()));
        ENTROPETIC_BLOCKS.add(toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        CabalistItems.registerItem(name, () -> new BlockItem(toReturn.get(), new Item.Properties()));
        return toReturn;
    }

    public static Collection<RegistryObject<Block>> getBlocks() {
        return BLOCKS.getEntries();
    }

    public static void bootstrap() {}
}
