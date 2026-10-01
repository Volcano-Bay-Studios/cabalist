package xyz.volcanobay.cabalist.core;

import foundry.veil.api.resource.VeilDynamicRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.render.AspectRenderSpec;
import xyz.volcanobay.cabalist.system.render.RenderLayer;
import xyz.volcanobay.cabalist.system.render.RenderSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class CabalistRenderSpecs {
    public static final ResourceKey<Registry<AspectRenderSpec>> RENDER_SPECS_KEY = ResourceKey.createRegistryKey(Cabalist.id("render_spec"));

    private static final List<RegistryDataLoader.RegistryData<?>> REGISTRIES = List.of(
            new RegistryDataLoader.RegistryData<>(RENDER_SPECS_KEY, AspectRenderSpec.CODEC, false)
    );

    private static Map<ResourceLocation, RenderSpec> byAspect = Map.of();

    private CabalistRenderSpecs() {
    }

    public static RenderSpec get(Aspect aspect) {
        ResourceLocation id = CabalistAspects.ASPECT_REGISTRY.getKey(aspect);
        return id == null ? RenderSpec.EMPTY : byAspect.getOrDefault(id, RenderSpec.EMPTY);
    }

    private static RenderSpec merge(RenderSpec first, RenderSpec second) {
        List<ResourceLocation> particles = new ArrayList<>(first.particles());
        particles.addAll(second.particles());
        List<RenderLayer> layers = new ArrayList<>(first.layers());
        layers.addAll(second.layers());
        int glyphColor = first.glyphColor() != RenderSpec.NO_COLOR ? first.glyphColor() : second.glyphColor();
        return new RenderSpec(List.copyOf(particles), List.copyOf(layers), glyphColor, first.barrier() || second.barrier());
    }

    public static class Reloader implements PreparableReloadListener {
        public static final Reloader INSTANCE = new Reloader();

        @Override
        public @NotNull CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller preparationsProfiler, @NotNull ProfilerFiller reloadProfiler, @NotNull Executor backgroundExecutor, @NotNull Executor gameExecutor) {
            return VeilDynamicRegistry.loadRegistries(resourceManager, REGISTRIES, backgroundExecutor)
                    .thenCompose(preparationBarrier::wait)
                    .thenAcceptAsync(data -> {
                        String errors = VeilDynamicRegistry.printErrors(data.errors());
                        if (errors != null) {
                            Cabalist.LOGGER.error("Render spec loading errors:{}", errors);
                        }
                        Map<ResourceLocation, RenderSpec> loaded = new HashMap<>();
                        for (AspectRenderSpec entry : data.registryAccess().registryOrThrow(RENDER_SPECS_KEY)) {
                            if (!CabalistAspects.ASPECT_REGISTRY.containsKey(entry.aspect())) {
                                Cabalist.LOGGER.error("Render spec targets unknown aspect {}", entry.aspect());
                                continue;
                            }
                            loaded.merge(entry.aspect(), entry.spec(), CabalistRenderSpecs::merge);
                        }
                        byAspect = Map.copyOf(loaded);
                    }, gameExecutor);
        }

        @Override
        public @NotNull String getName() {
            return CabalistRenderSpecs.class.getSimpleName();
        }
    }
}
