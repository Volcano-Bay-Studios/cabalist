package xyz.volcanobay.cabalist.core.data;

import net.minecraft.core.Direction;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.*;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import oshi.util.tuples.Pair;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.core.CabalistBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CabalistBlockModelProvider extends BlockStateProvider implements DataProvider {
    public CabalistBlockModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Cabalist.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlock(CabalistBlocks.OSCILISTONE.get());
        simpleBlock(CabalistBlocks.SUPERHEATED_SAND.get());
    }

    public ModelFile.ExistingModelFile existingFile(String file, String... path) {
        StringBuilder finalPath = new StringBuilder();
        finalPath.append("block/");
        for (String subpath : path) {
            finalPath.append(subpath).append("/");
        }
        finalPath.append(file);
        return models().getExistingFile(Cabalist.id(finalPath.toString()));
    }

    public ModelFile.ExistingModelFile existingOctFile(String file, String... path) {
        StringBuilder finalPath = new StringBuilder();
        finalPath.append("oct/");
        for (String subpath : path) {
            finalPath.append(subpath).append("/");
        }
        finalPath.append(file);
        return models().getExistingFile(Cabalist.id(finalPath.toString()));
    }

    public ModelFile.ExistingModelFile existingItemFile(String file, String... path) {
        String finalPath = this.getPath("item", file, path);
        return itemModels().getExistingFile(Cabalist.id(finalPath));
    }

    private float getU(int index) {
        return (index % 8) * 2f;
    }

    private float getV(int index) {
        return (float) (Math.floor(index / 8f) * 2f);
    }

    private int getSpriteIndex(Direction face, boolean n, boolean s, boolean e, boolean w, boolean u, boolean d) {
        boolean hasUp, hasDown, hasLeft, hasRight;

        switch (face) {
            case NORTH -> {
                hasUp = u;
                hasDown = d;
                hasLeft = e;
                hasRight = w;
            }
            case SOUTH -> {
                hasUp = u;
                hasDown = d;
                hasLeft = w;
                hasRight = e;
            }
            case EAST -> {
                hasUp = u;
                hasDown = d;
                hasLeft = s;
                hasRight = n;
            }
            case WEST -> {
                hasUp = u;
                hasDown = d;
                hasLeft = n;
                hasRight = s;
            }
            case UP -> {
                hasUp = n;
                hasDown = s;
                hasLeft = w;
                hasRight = e;
            }
            case DOWN -> {
                hasUp = s;
                hasDown = n;
                hasLeft = w;
                hasRight = e;
            }
            default -> {
                return 0;
            }
        }

        int count = (hasUp ? 1 : 0) + (hasDown ? 1 : 0) + (hasLeft ? 1 : 0) + (hasRight ? 1 : 0);

        if (count == 0) return 0;
        if (count == 4) return 11;

        if (count == 1) {
            if (hasRight) return 1;
            if (hasUp) return 2;
            if (hasLeft) return 3;
            return 4; // Down
        }

        if (count == 2) {
            if (hasLeft && hasRight) return 5; // Horiz
            if (hasUp && hasDown) return 6; // Vert
            if (hasRight && hasUp) return 12;
            if (hasUp && hasLeft) return 13;
            if (hasLeft && hasDown) return 14;
            return 15; // Down-Right
        }

        if (count == 3) {
            if (!hasLeft) return 7;  // T-Right
            if (!hasDown) return 8;  // T-Up
            if (!hasRight) return 9;  // T-Left
            return 10; // T-Down
        }
        return 0;
    }

    public String getPath(String prefix, String file, String... path) {
        StringBuilder finalPath = new StringBuilder();
        finalPath.append(prefix).append("/");
        for (String subpath : path) {
            finalPath.append(subpath).append("/");
        }
        finalPath.append(file);
        return finalPath.toString();
    }
}
