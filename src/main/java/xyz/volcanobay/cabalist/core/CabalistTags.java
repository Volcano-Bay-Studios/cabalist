package xyz.volcanobay.cabalist.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import xyz.volcanobay.cabalist.Cabalist;

public class CabalistTags {
    public static final TagKey<Block> ENTROPETIC = TagKey.create(
            Registries.BLOCK,
            Cabalist.id("entropetic")
    );

    public static void bootstrap(){

    }
}
