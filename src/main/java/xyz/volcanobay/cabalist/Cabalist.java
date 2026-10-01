package xyz.volcanobay.cabalist;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.entity.NoopRenderer;
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
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.slf4j.Logger;
import xyz.volcanobay.cabalist.client.renderer.entity.LifeforceGlyphRenderer;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphParticle;
import xyz.volcanobay.cabalist.client.renderer.item.LifeforceGlint;
import xyz.volcanobay.cabalist.client.tooltip.ClientInscriptionTooltip;
import xyz.volcanobay.cabalist.client.tooltip.InscriptionTooltip;
import xyz.volcanobay.cabalist.core.*;
import xyz.volcanobay.cabalist.core.data.CabalistBlockModelProvider;
import xyz.volcanobay.cabalist.core.data.CabalistItemModelProvider;
import xyz.volcanobay.cabalist.core.data.CabalistLanguageProvider;
import xyz.volcanobay.cabalist.core.data.CabalistTagsProvider;
import xyz.volcanobay.cabalist.networking.CabalistMessages;
import xyz.volcanobay.cabalist.system.casting.CastingSystem;
import xyz.volcanobay.voicelib.VoiceLibClient;
import xyz.volcanobay.voicelib.api.VoiceLibApi;

@Mod(Cabalist.MODID)
public class Cabalist {
    public static final String MODID = "cabalist";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Cabalist(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, CabalistConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, CabalistAspects.CONFIG, MODID + "-aspects.toml");

        CabalistSpatialNetworks.bootstrap();
        CabalistBlocks.bootstrap();
        CabalistBlockEntities.bootstrap();
        CabalistItems.bootstrap();
        CabalistCreativeModeTab.bootstrap();
        CabalistTags.bootstrap();
        CabalistTerms.bootstrap();
        CabalistModifiers.bootstrap();
        CabalistSpellComponents.bootstrap();
        CabalistSpellDictionary.bootstrap();
        CabalistMessages.register();
        VoiceLibApi.registerServerPlayerSpeechListener(event -> event.getPlayer().server.execute(() -> CastingSystem.INSTANCE.speak(event.getPlayer(), event.getText(), true)));
        VoiceLibApi.registerServerPlayerPartialSpeechListener(event -> event.getPlayer().server.execute(() -> CastingSystem.INSTANCE.speak(event.getPlayer(), event.getText(), false)));
        CabalistEnergyTypes.bootstrap();
        CabalistAspects.bootstrap();
        CabalistForms.bootstrap();
        CabalistEntities.bootstrap();
        CabalistParticles.bootstrap();
        CabalistDataComponents.bootstrap();
        CabalistGameEvents.bootstrap();
        CabalistAttachments.bootstrap(modEventBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            VoiceLibClient.printToConsole = true;
        }

        @SubscribeEvent
        public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(InscriptionTooltip.class, ClientInscriptionTooltip::new);
        }

        @SubscribeEvent
        public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
            event.registerSpecial(CabalistParticles.GLYPH.get(), new GlyphParticle.Provider());
        }

        @SubscribeEvent
        public static void registerRenderBuffers(RegisterRenderBuffersEvent event) {
            LifeforceGlint.register(event);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(CabalistEntities.SPELL_PROJECTILE.get(), NoopRenderer::new);
            event.registerEntityRenderer(CabalistEntities.SKY_STRIKE.get(), NoopRenderer::new);
            event.registerEntityRenderer(CabalistEntities.LIFEFORCE_GLYPH.get(), LifeforceGlyphRenderer::new);
        }
    }

    @EventBusSubscriber(modid = MODID)
    public static class CommonModEvents {
        @SubscribeEvent
        public static void gatherData(GatherDataEvent event) {
            DataGenerator generator = event.getGenerator();
            PackOutput output = generator.getPackOutput();
            ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
            ResourceManager serverData = event.getResourceManager(PackType.SERVER_DATA);
            generator.addProvider(event.includeClient(), new CabalistBlockModelProvider(output, existingFileHelper));
            generator.addProvider(event.includeClient(), new CabalistItemModelProvider(output, existingFileHelper));
            generator.addProvider(event.includeClient(), new CabalistLanguageProvider(output, "en_us", serverData));
            generator.addProvider(event.includeServer(), new CabalistTagsProvider(output, event.getLookupProvider(), existingFileHelper));
        }
    }
}
