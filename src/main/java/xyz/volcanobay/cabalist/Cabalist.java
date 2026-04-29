package xyz.volcanobay.cabalist;

import com.mojang.logging.LogUtils;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import xyz.volcanobay.cabalist.core.*;
import xyz.volcanobay.cabalist.core.data.CabalistBlockModelProvider;
import xyz.volcanobay.cabalist.core.data.CabalistItemModelProvider;
import xyz.volcanobay.cabalist.core.data.CabalistLanguageProvider;
import xyz.volcanobay.cabalist.core.data.CabalistTagsProvider;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Cabalist.MODID)
public class Cabalist {
    public static final String MODID = "cabalist";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Cabalist(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, CabalistConfig.SPEC);

        CabalistSpatialNetworks.bootstrap();
        CabalistBlocks.bootstrap();
        CabalistItems.bootstrap();
        CabalistCreativeModeTab.bootstrap();
        CabalistTags.bootstrap();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID,path);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }


        @SubscribeEvent
        public static void gatherData(GatherDataEvent event) {
            DataGenerator generator = event.getGenerator();
            PackOutput output = generator.getPackOutput();
            ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
            ResourceManager serverData = event.getResourceManager(PackType.SERVER_DATA);



            generator.addProvider(
                    event.includeClient(),
                    new CabalistBlockModelProvider(output, existingFileHelper)
            );

            generator.addProvider(
                    event.includeClient(),
                    new CabalistItemModelProvider(output, existingFileHelper)
            );

            generator.addProvider(
                    event.includeClient(),
                    new CabalistLanguageProvider(output, "en_us", serverData)
            );
        }
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CommonModEvents {
        @SubscribeEvent
        public static void gatherData(GatherDataEvent event) {
            DataGenerator generator = event.getGenerator();
            PackOutput output = generator.getPackOutput();
            ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
            ResourceManager serverData = event.getResourceManager(PackType.SERVER_DATA);

            generator.addProvider(
                    event.includeServer(),
                    new CabalistTagsProvider(output,  event.getLookupProvider(), existingFileHelper)
            );
        }
    }
}
