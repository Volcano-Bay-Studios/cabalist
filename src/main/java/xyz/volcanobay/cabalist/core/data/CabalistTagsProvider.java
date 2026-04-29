package xyz.volcanobay.cabalist.core.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.core.CabalistBlocks;
import xyz.volcanobay.cabalist.core.CabalistTags;

import java.util.concurrent.CompletableFuture;

public class CabalistTagsProvider extends BlockTagsProvider {
    public CabalistTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Cabalist.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        IntrinsicTagAppender<Block> tag = tag(CabalistTags.ENTROPETIC);
        CabalistBlocks.ENTROPETIC_BLOCKS.forEach(registryObject -> {
            tag.add(registryObject.get());
        });
    }
}
