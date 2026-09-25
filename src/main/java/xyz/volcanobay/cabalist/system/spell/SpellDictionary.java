package xyz.volcanobay.cabalist.system.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.voicelib.api.util.PhoneticComparison;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SpellDictionary {
    private static final double PHONETIC_CONFIDENCE_THRESHOLD = 0.85;
    private static final int MAX_CACHED_PHRASES = 4096;

    public final Map<String, Double> text;
    private final int maxWordCount;
    private final Map<String, Double> similarityCache = new ConcurrentHashMap<>();
    private final Optional<ResourceLocation> componentLocation;
    private final List<ResourceLocation> components;
    private List<SpellComponent> spellComponents = List.of();

    public static final Codec<SpellDictionary> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.DOUBLE
            ).fieldOf("words").forGetter(SpellDictionary::getText),
            ResourceLocation.CODEC.optionalFieldOf("component").forGetter(SpellDictionary::getComponentLocation),
            Codec.list(ResourceLocation.CODEC).optionalFieldOf("components", List.of()).forGetter(SpellDictionary::getComponents)
    ).apply(instance, SpellDictionary::new));

    public SpellDictionary(Map<String, Double> words, Optional<ResourceLocation> location, List<ResourceLocation> components) {
        this.componentLocation = location;
        this.components = components;
        Map<String, Double> normalized = new HashMap<>();
        for (String word : words.keySet()) {
            normalized.put(normalize(word), words.get(word));
        }
        this.text = normalized;

        int max = 1;
        for (String word : this.text.keySet()) {
            max = Math.max(max, word.split(" ").length);
        }
        this.maxWordCount = max;
    }

    private Optional<ResourceLocation> getComponentLocation() {
        return componentLocation;
    }

    private List<ResourceLocation> getComponents() {
        return components;
    }

    public void resolveSpellComponents(ResourceLocation dictionaryLocation) {
        List<SpellComponent> resolved = new ArrayList<>();
        for (ResourceLocation component : components) {
            addSpellComponent(resolved, component, dictionaryLocation);
        }
        if (componentLocation.isPresent()) {
            addSpellComponent(resolved, componentLocation.get(), dictionaryLocation);
        }
        spellComponents = List.copyOf(resolved);
    }

    private static void addSpellComponent(List<SpellComponent> resolved, ResourceLocation component, ResourceLocation dictionaryLocation) {
        SpellComponent spellComponent = CabalistSpellComponents.PART_REGISTRY.get(component);
        if (spellComponent == null) {
            Cabalist.LOGGER.error("Spell dictionary {} references unknown component {}", dictionaryLocation, component);
            return;
        }
        resolved.add(spellComponent);
    }

    public List<SpellComponent> getSpellComponents() {
        return spellComponents;
    }

    public Map<String, Double> getText() {
        return text;
    }

    public int getMaxWordCount() {
        return maxWordCount;
    }

    public double getSimilarity(String phrase) {
        if (similarityCache.size() > MAX_CACHED_PHRASES) {
            similarityCache.clear();
        }
        return similarityCache.computeIfAbsent(normalize(phrase), this::computeSimilarity);
    }

    private double computeSimilarity(String phrase) {
        double bestPhonetic = 0;
        for (String word : text.keySet()) {
            bestPhonetic = Math.max(bestPhonetic, PhoneticComparison.calculate(phrase, word));
        }
        if (bestPhonetic >= PHONETIC_CONFIDENCE_THRESHOLD) {
            return bestPhonetic;
        }

        double bestSemantic = 0;
//        for (String word : words.keySet()) {
//            bestSemantic = Math.max(bestSemantic, WordEmbeddings.INSTANCE.similarity(phrase,word));
//        }
        return Math.max(bestPhonetic, bestSemantic);
    }

    private static String normalize(String text) {
        return text.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", "");
    }
}
