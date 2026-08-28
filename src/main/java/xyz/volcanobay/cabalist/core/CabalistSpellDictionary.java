package xyz.volcanobay.cabalist.core;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import foundry.veil.api.quasar.data.QuasarParticles;
import foundry.veil.api.resource.VeilDynamicRegistry;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.spell.SpellDictionary;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class CabalistSpellDictionary {
    public static final ResourceKey<Registry<SpellDictionary>> SPELL_DICTIONARIES_KEY = createRegistryKey("spell_dictionary");

    private static final List<RegistryDataLoader.RegistryData<?>> REGISTRIES = List.of(
            new RegistryDataLoader.RegistryData<>(SPELL_DICTIONARIES_KEY, SpellDictionary.CODEC, false)
    );

    private static RegistryAccess registryAccess = RegistryAccess.EMPTY;

    private static final SuggestionProvider<?> SPELL_DICTIONARIES_SUGGESTION_PROVIDER = (unused, builder) -> registryAccess().registry(SPELL_DICTIONARIES_KEY).map(registry -> SharedSuggestionProvider.suggestResource(registry.keySet(), builder)).orElseGet(Suggestions::empty);

    private CabalistSpellDictionary() {
    }

    public static void bootstrap() {
    }

    public static Registry<SpellDictionary> getRegistry() {
        Optional<Registry<SpellDictionary>> registry = CabalistSpellDictionary.registryAccess().registry(CabalistSpellDictionary.SPELL_DICTIONARIES_KEY);
        return registry.orElse(null);
    }

    public static SpellDictionary getDictionary(String name) {
        Registry<SpellDictionary> registry = getRegistry();
        return registry.get(Cabalist.id(name));
    }

    @SuppressWarnings("unchecked")
    public static <T extends SharedSuggestionProvider> SuggestionProvider<T> spellDictionarysSuggestionProvider() {
        return (SuggestionProvider<T>) SPELL_DICTIONARIES_SUGGESTION_PROVIDER;
    }

    private static <T> ResourceKey<Registry<T>> createRegistryKey(String name) {
        return ResourceKey.createRegistryKey(Cabalist.id(name));
    }

    public static RegistryAccess registryAccess() {
        return registryAccess;
    }

    public static @Nullable SpellDictionary getSpellDictionary(String name) {
        return getSpellDictionary(ResourceLocation.parse(name));
    }

    public static @Nullable SpellDictionary getSpellDictionary(ResourceLocation location) {
        Optional<Registry<SpellDictionary>> registry = registryAccess().registry(SPELL_DICTIONARIES_KEY);
        return registry.map(spellDictionaries -> spellDictionaries.get(location)).orElse(null);
    }

    public static class Reloader implements PreparableReloadListener {

        public static final Reloader INSTANCE = new Reloader();

        public Reloader() {
        }

        @Override
        public @NotNull CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller preparationsProfiler, @NotNull ProfilerFiller reloadProfiler, @NotNull Executor backgroundExecutor, @NotNull Executor gameExecutor) {
            return VeilDynamicRegistry.loadRegistries(resourceManager, REGISTRIES, backgroundExecutor)
                    .thenCompose(preparationBarrier::wait)
                    .thenAcceptAsync(data -> {
                        registryAccess = data.registryAccess();
                        data.errors().values().forEach(Exception::printStackTrace);
                        String msg = VeilDynamicRegistry.printErrors(data.errors());
                        if (msg != null) {
                            Cabalist.LOGGER.error("SpellDictionary registry loading errors:{}", msg);
                        }
                        Cabalist.LOGGER.info("Loaded {} spellDictionarys", registryAccess.registryOrThrow(SPELL_DICTIONARIES_KEY).size());
                    }, gameExecutor);
        }

        @Override
        public @NotNull String getName() {
            return QuasarParticles.class.getSimpleName();
        }
    }
}
