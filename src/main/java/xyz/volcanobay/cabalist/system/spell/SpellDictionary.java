package xyz.volcanobay.cabalist.system.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import edu.uniba.di.lacam.kdde.ws4j.WS4J;
import xyz.volcanobay.voicelib.api.util.PhoneticComparison;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpellDictionary {
    private static final double PHONETIC_CONFIDENCE_THRESHOLD = 0.85;

    public final Map<String, Double> words;
    private final int maxWordCount;
    private final Map<String, Double> similarityCache = new ConcurrentHashMap<>();

    public SpellDictionary(Map<String, Double> words) {
        Map<String,Double> normalized = new HashMap<>();
        for (String word : words.keySet()) {
            normalized.put(normalize(word), words.get(word));
        }
        this.words = normalized;

        int max = 1;
        for (String word : this.words.keySet()) {
            max = Math.max(max, word.split(" ").length);
        }
        this.maxWordCount = max;
    }

    public static final Codec<SpellDictionary> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.DOUBLE
            ).fieldOf("words").forGetter(SpellDictionary::getWords)
    ).apply(instance, SpellDictionary::new));

    public Map<String, Double> getWords() {
        return words;
    }

    public int getMaxWordCount() {
        return maxWordCount;
    }

    public double getSimilarity(String phrase) {
        return similarityCache.computeIfAbsent(normalize(phrase), this::computeSimilarity);
    }

    private double computeSimilarity(String phrase) {
        double bestPhonetic = 0;
        for (String word : words.keySet()) {
            bestPhonetic = Math.max(bestPhonetic, PhoneticComparison.calculate(phrase, word));
        }
        if (bestPhonetic >= PHONETIC_CONFIDENCE_THRESHOLD) {
            return bestPhonetic;
        }

        double bestSemantic = 0;
        for (String word : words.keySet()) {
            bestSemantic = Math.max(bestSemantic, WS4J.runHSO(phrase, word) / 16d);
        }
        return Math.max(bestPhonetic, bestSemantic);
    }

    public double getPower(String normalizedKey) {
        return words.getOrDefault(normalizedKey,0d);
    }

    private static String normalize(String text) {
        return text.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", "");
    }
}
