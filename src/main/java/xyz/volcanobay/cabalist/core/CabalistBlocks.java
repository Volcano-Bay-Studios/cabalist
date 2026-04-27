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
import xyz.volcanobay.cabalist.content.blocks.OscilistoneBlock;

import java.util.function.Supplier;

public class CabalistBlocks {
    public static final RegistrationProvider<Block> BLOCKS = RegistrationProvider.get(Registries.BLOCK, Cabalist.MODID);
    public static final RegistryObject<OscilistoneBlock> OSCILISTONE = registerBlock("oscilistone",
            () -> new OscilistoneBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
                    .strength(2.0f, 8f).noOcclusion()
            ));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        CabalistItems.registerItem(name, () -> new BlockItem(toReturn.get(), new Item.Properties()));
        return toReturn;
    }

    public static void register() {}
}
