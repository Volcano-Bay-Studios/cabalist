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

//    protected void registerObjModels() {
//        simpleObj("cassini:block/control_panel", "block", "control_panel");
//    }

    public ModelFile.ExistingModelFile existingFile(String file, String... path) {
        StringBuilder finalPath = new StringBuilder();
        finalPath.append("block/");
        for (String subpath : path) {
            finalPath.append(subpath).append("/");
        }
        finalPath.append(file);
        return models().getExistingFile(Cabalist.id(finalPath.toString()));
    }

//    public ModelFile.ExistingModelFile existingObjFile(String file, String... path) {
//        String finalPath = this.getPath("block", file, path);
//        return objModels.getExistingFile(Cassini.id(finalPath));
//    }

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
//
//    public void simpleObj(String texture, String file, String... path) {
//        String finalPath = this.getPath("block", file, path);
//        objModels.getBuilder(finalPath)
//                .modelLocation(Cassini.id("models/" + finalPath + ".obj"))
//                .overrideMaterialLibrary(Cassini.id("models/" + finalPath + ".mtl"))
//                .automaticCulling(true)
//                .shadeQuads(true)
//                .flipV(true)
//                .emissiveAmbient(true)
//                .end()
//                .texture("0", texture)
//                .texture("particle", texture);
//
//    }
//
//
//    private void registerPipe() {
//        Block block = CassiniBlocks.METAL_OCT_PIPE.get();
//        VariantBlockStateBuilder builder = getVariantBuilder(block);
//
//        block.getStateDefinition().getPossibleStates().forEach(state -> {
//            boolean n = state.getValue(BlockStateProperties.NORTH);
//            boolean s = state.getValue(BlockStateProperties.SOUTH);
//            boolean e = state.getValue(BlockStateProperties.EAST);
//            boolean w = state.getValue(BlockStateProperties.WEST);
//            boolean u = state.getValue(BlockStateProperties.UP);
//            boolean d = state.getValue(BlockStateProperties.DOWN);
//
//            String name = "pipe_" + (n ? "n" : "") + (s ? "s" : "") + (e ? "e" : "") + (w ? "w" : "") + (u ? "u" : "") + (d ? "d" : "");
//            if (name.equals("pipe_")) name = "pipe_none";
//
//            BlockModelBuilder model = models().withExistingParent("oct/pipe/" + name, "block/block")
//                    .texture("all", modLoc("block/pipe/metal_pipe_sheet"))
//                    .texture("particle", modLoc("block/pipe/metal_pipe_sheet"));
//
//            List<Pair<Direction, ModelBuilder<BlockModelBuilder>.ElementBuilder>> elements = new ArrayList<>();
//
//            elements.add(new Pair<>(null, model.element().from(5, 5, 5).to(11, 11, 11)));
//
//            if (n) {
//                elements.add(new Pair<>(Direction.NORTH, model.element().from(5, 5, 11).to(11, 11, 12)));
//            }
//            if (s) {
//                elements.add(new Pair<>(Direction.SOUTH, model.element().from(5, 5, 4).to(11, 11, 5)));
//            }
//            if (w) {
//                elements.add(new Pair<>(Direction.WEST, model.element().from(11, 5, 5).to(12, 11, 11)));
//            }
//            if (e) {
//                elements.add(new Pair<>(Direction.EAST, model.element().from(4, 5, 5).to(5, 11, 11)));
//            }
//            if (d) {
//                elements.add(new Pair<>(Direction.DOWN, model.element().from(5, 11, 5).to(11, 12, 11)));
//            }
//            if (u) {
//                elements.add(new Pair<>(Direction.UP, model.element().from(5, 4, 5).to(11, 5, 11)));
//            }
//
//            for (Pair<Direction, ModelBuilder<BlockModelBuilder>.ElementBuilder> pair : elements) {
//                Direction direction = pair.getA();
//                ModelBuilder<BlockModelBuilder>.ElementBuilder element = pair.getB();
//                for (Direction side : Direction.values()) {
//                    int idx = getSpriteIndex(side, n, s, e, w, u, d);
//                    if (direction != null) {
//                        boolean hasUp = false, hasDown = false, hasLeft = false, hasRight = false;
//
//                        switch (side) {
//                            case NORTH -> {
//                                hasUp = direction == Direction.UP;
//                                hasDown = direction == Direction.DOWN;
//                                hasLeft = direction == Direction.EAST;
//                                hasRight = direction == Direction.WEST;
//                            }
//                            case SOUTH -> {
//                                hasUp = direction == Direction.UP;
//                                hasDown = direction == Direction.DOWN;
//                                hasLeft = direction == Direction.WEST;
//                                hasRight = direction == Direction.EAST;
//                            }
//                            case EAST -> {
//                                hasUp = direction == Direction.UP;
//                                hasDown = direction == Direction.DOWN;
//                                hasLeft = direction == Direction.SOUTH;
//                                hasRight = direction == Direction.NORTH;
//                            }
//                            case WEST -> {
//                                hasUp = direction == Direction.UP;
//                                hasDown = direction == Direction.DOWN;
//                                hasLeft = direction == Direction.NORTH;
//                                hasRight = direction == Direction.SOUTH;
//                            }
//                            case UP -> {
//                                hasUp = direction == Direction.NORTH;
//                                hasDown = direction == Direction.SOUTH;
//                                hasLeft = direction == Direction.WEST;
//                                hasRight = direction == Direction.EAST;
//                            }
//                            case DOWN -> {
//                                hasUp = direction == Direction.SOUTH;
//                                hasDown = direction == Direction.NORTH;
//                                hasLeft = direction == Direction.WEST;
//                                hasRight = direction == Direction.EAST;
//                            }
//                        }
//                        float leftX;
//                        float rightX;
//                        if (hasRight) {
//                            leftX = 0;
//                            rightX = 0.25f;
//                        } else if (hasLeft) {
//                            leftX = 1.75f;
//                            rightX = 2f;
//                        } else {
//                            leftX = 0.25f;
//                            rightX = 1.75f;
//                        }
//
//                        float leftY;
//                        float rightY;
//                        if (hasDown) {
//                            leftY = 0;
//                            rightY = 0.25f;
//                        } else if (hasUp) {
//                            leftY = 1.75f;
//                            rightY = 2f;
//                        } else {
//                            leftY = 0.25f;
//                            rightY = 1.75f;
//                        }
//
//                            element.face(side)
//                                .uvs(getU(idx) + leftX, getV(idx) + leftY, getU(idx) + rightX, getV(idx) + rightY)
//                                .texture("#all")
//                                .end();
//                    } else {
//                        element.face(side)
//                                .uvs(getU(idx) + 0.25f, getV(idx) + 0.25f, getU(idx) + 1.75f, getV(idx) + 1.75f)
//                                .texture("#all")
//                                .end();
//                    }
//                }
//                element.end();
//            }
//
//            builder.partialState()
//                    .with(BlockStateProperties.NORTH, n).with(BlockStateProperties.SOUTH, s)
//                    .with(BlockStateProperties.EAST, e).with(BlockStateProperties.WEST, w)
//                    .with(BlockStateProperties.UP, u).with(BlockStateProperties.DOWN, d)
//                    .addModels(new ConfiguredModel(model));
//        });
//    }


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
