package xyz.volcanobay.cabalist.system.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.voicelib.api.util.PhoneticComparison;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpellDictionary {
    private static final double PHONETIC_CONFIDENCE_THRESHOLD = 0.85;

    public final Map<String, Double> text;
    private final int maxWordCount;
    private final Map<String, Double> similarityCache = new ConcurrentHashMap<>();
    private final ResourceLocation componentLocation;

    public SpellDictionary(Map<String, Double> words, ResourceLocation location) {
        this.componentLocation = location;
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

    public static final Codec<SpellDictionary> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.DOUBLE
            ).fieldOf("words").forGetter(SpellDictionary::getText),
            ResourceLocation.CODEC.fieldOf("component").forGetter(SpellDictionary::getComponentLocation)
    ).apply(instance, SpellDictionary::new));

    public ResourceLocation getComponentLocation() {
        return componentLocation;
    }

    public SpellComponent getComponent() {
        return CabalistSpellComponents.PART_REGISTRY.get(componentLocation);
    }

    public Map<String, Double> getText() {
        return text;
    }

    public int getMaxWordCount() {
        return maxWordCount;
    }

    public double getSimilarity(String phrase) {
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

    public double getPower(String normalizedKey) {
        return text.getOrDefault(normalizedKey, 0d);
    }

    private static String normalize(String text) {
        return text.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", "");
    }
}
