package xyz.volcanobay.cabalist.core.data;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.core.CabalistBlocks;

public class CabalistItemModelProvider extends ItemModelProvider {


    public CabalistItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Cabalist.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        block("oscilistone");
    }

    private void block(String name) {
        this.getBuilder(name).parent(existingBlockFile(name));
    }


    public ModelFile.ExistingModelFile existingFile(String file, String... path) {
        StringBuilder finalPath = new StringBuilder();
        finalPath.append("item/");
        for (String subpath : path) {
            finalPath.append(subpath).append("/");
        }
        finalPath.append(file);
        return this.getExistingFile(Cabalist.id(finalPath.toString()));
    }

    public ModelFile.ExistingModelFile existingBlockFile(String file, String... path) {
        StringBuilder finalPath = new StringBuilder();
        finalPath.append("block/");
        for (String subpath : path) {
            finalPath.append(subpath).append("/");
        }
        finalPath.append(file);
        return this.getExistingFile(Cabalist.id(finalPath.toString()));
    }

//    public ModelFile.ExistingModelFile existingObjFile(String file, String... path) {
//        String finalPath = this.getPath("block", file, path);
//        return objModels.getExistingFile(Cabalist.id(finalPath));
//    }

    public ModelFile.ExistingModelFile existingItemFile(String file, String... path) {
        String finalPath = this.getPath("item", file, path);
        return this.getExistingFile(Cabalist.id(finalPath));
    }

//    public void simpleObj(String texture, String file, String... path) {
//        String finalPath = this.getPath("block", file, path);
//        objModels.getBuilder(finalPath)
//                .modelLocation(Cabalist.id("models/" + finalPath + ".obj"))
//                .overrideMaterialLibrary(Cabalist.id("models/" + finalPath + ".mtl"))
//                .automaticCulling(true)
//                .shadeQuads(true)
//                .flipV(true)
//                .emissiveAmbient(true)
//                .end()
//                .texture("0", texture)
//                .texture("particle", texture);
//
//    }

//    public void simpleItemObj(String texture, String file, String... path) {
//        String finalPath = this.getPath("block", file, path);
//        objModels.getBuilder(finalPath)
//                .modelLocation(Cabalist.id("models/" + finalPath + ".obj"))
//                .overrideMaterialLibrary(Cabalist.id("models/" + finalPath + ".mtl"))
//                .automaticCulling(true)
//                .shadeQuads(true)
//                .flipV(false)
//                .emissiveAmbient(true)
//                .end()
//                .texture("0", texture)
//                .texture("particle", texture);
//
//    }

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
