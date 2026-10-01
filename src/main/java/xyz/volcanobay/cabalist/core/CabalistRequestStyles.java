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
import xyz.volcanobay.cabalist.system.request.RequestStyle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class CabalistRequestStyles {
    public static final ResourceKey<Registry<RequestStyle>> REQUEST_STYLES_KEY = ResourceKey.createRegistryKey(Cabalist.id("request_style"));

    private static final List<RegistryDataLoader.RegistryData<?>> REGISTRIES = List.of(
            new RegistryDataLoader.RegistryData<>(REQUEST_STYLES_KEY, RequestStyle.CODEC, false)
    );

    private static Map<ResourceLocation, RequestStyle> styles = Map.of();

    private CabalistRequestStyles() {
    }

    public static RequestStyle get(String name) {
        return styles.getOrDefault(Cabalist.id(name), RequestStyle.DEFAULT);
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
                            Cabalist.LOGGER.error("Request style loading errors:{}", errors);
                        }
                        Map<ResourceLocation, RequestStyle> loaded = new HashMap<>();
                        data.registryAccess().registryOrThrow(REQUEST_STYLES_KEY).entrySet()
                                .forEach(entry -> loaded.put(entry.getKey().location(), entry.getValue()));
                        styles = Map.copyOf(loaded);
                    }, gameExecutor);
        }

        @Override
        public @NotNull String getName() {
            return CabalistRequestStyles.class.getSimpleName();
        }
    }
}
