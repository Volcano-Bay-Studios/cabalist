package xyz.volcanobay.cabalist.system.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
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

    private static final double WORD_MATCH_SIMILARITY = 0.7;
    private static final double PHRASE_BONUS = 1.5;
    private static final double MISMATCH_PENALTY = 1;
    private static final double EXACT_MATCH_BONUS = 0.05;

    private static final Match NO_MATCH = new Match(null, 0, 0);

    public final Map<String, Double> text;
    private final List<String> ignored;
    private final int maxWordCount;
    private final Map<String, Match> similarityCache = new ConcurrentHashMap<>();
    private final Map<String, String[]> splitEntries = new HashMap<>();
    private final Optional<ResourceLocation> componentLocation;
    private final List<ResourceLocation> components;
    private List<SpellComponent> spellComponents = List.of();

    public static final Codec<SpellDictionary> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(
                    Codec.STRING,
                    Codec.DOUBLE
            ).fieldOf("words").forGetter(SpellDictionary::getText),
            Codec.STRING.listOf().optionalFieldOf("ignore", List.of()).forGetter(SpellDictionary::getIgnored),
            ResourceLocation.CODEC.optionalFieldOf("component").forGetter(SpellDictionary::getComponentLocation),
            Codec.list(ResourceLocation.CODEC).optionalFieldOf("components", List.of()).forGetter(SpellDictionary::getComponents)
    ).apply(instance, SpellDictionary::new));

    public SpellDictionary(Map<String, Double> words, List<String> ignored, Optional<ResourceLocation> location, List<ResourceLocation> components) {
        this.componentLocation = location;
        this.components = components;
        Map<String, Double> normalized = new HashMap<>();
        for (String word : words.keySet()) {
            normalized.put(normalize(word), words.get(word));
        }
        this.text = normalized;
        List<String> normalizedIgnored = new ArrayList<>();
        for (String phrase : ignored) {
            normalizedIgnored.add(normalize(phrase));
        }
        this.ignored = List.copyOf(normalizedIgnored);

        int max = 1;
        for (String word : this.text.keySet()) {
            splitEntries.put(word, word.split(" "));
            max = Math.max(max, splitEntries.get(word).length);
        }
        for (String phrase : this.ignored) {
            splitEntries.put(phrase, phrase.split(" "));
            max = Math.max(max, splitEntries.get(phrase).length);
        }
        this.maxWordCount = max;
    }

    public List<String> getIgnored() {
        return ignored;
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
        return getMatch(phrase).similarity();
    }

    public Match getMatch(String phrase) {
        if (similarityCache.size() > MAX_CACHED_PHRASES) {
            similarityCache.clear();
        }
        return similarityCache.computeIfAbsent(normalize(phrase), this::computeMatch);
    }

    public double getWeight(String phrase) {
        return text.getOrDefault(phrase, 0.0);
    }

    private Match computeMatch(String phrase) {
        String[] phraseWords = phrase.split(" ");
        Match best = NO_MATCH;
        for (String word : text.keySet()) {
            Match match = compare(phraseWords, word);
            if (match != null && match.score() > best.score()) {
                best = match;
            }
        }
        for (String ignoredPhrase : ignored) {
            Match match = compare(phraseWords, ignoredPhrase);
            if (match != null && match.score() >= best.score()) {
                return NO_MATCH;
            }
        }
        if (best.similarity() >= PHONETIC_CONFIDENCE_THRESHOLD) {
            return best;
        }

        return best;
    }

    /**
     * Single words run their comparisons. Phrases will match the same amount of words. Phrases build bonuses as words match,
     * making them more likely to catch.
     */
    private @Nullable Match compare(String[] phraseWords, String entry) {
        String[] entryWords = splitEntries.get(entry);
        if (entryWords == null || entryWords.length != phraseWords.length) {
            return null;
        }
        if (phraseWords.length == 1) {
            double similarity = getWordSimilarity(phraseWords[0], entryWords[0]);
            return new Match(entry, similarity, similarity + getExactBonus(phraseWords[0], entryWords[0]));
        }
        double score = 0;
        double totalSimilarity = 0;
        int streak = 0;
        for (int i = 0; i < phraseWords.length; i++) {
            double similarity = getWordSimilarity(phraseWords[i], entryWords[i]);
            totalSimilarity += similarity;
            if (similarity >= WORD_MATCH_SIMILARITY) {
                score += similarity * Math.pow(PHRASE_BONUS, streak) + getExactBonus(phraseWords[i], entryWords[i]);
                streak++;
            } else {
                score -= MISMATCH_PENALTY;
                streak = 0;
            }
        }
        return new Match(entry, totalSimilarity / phraseWords.length, score);
    }

    private static double getWordSimilarity(String spoken, String entry) {
        if (spoken.equals(entry)) {
            return 1;
        }
        return PhoneticComparison.calculate(spoken, entry);
    }

    /**
     * This breaks ties between words that sound the same, like "then" and "thin".
     */
    private static double getExactBonus(String spoken, String entry) {
        return spoken.equals(entry) ? EXACT_MATCH_BONUS : 0;
    }

    public static double getExactScore(int words) {
        double score = 0;
        for (int i = 0; i < words; i++) {
            score += Math.pow(PHRASE_BONUS, i);
        }
        return score;
    }

    public record Match(@Nullable String phrase, double similarity, double score) {
    }

    private static String normalize(String text) {
        return text.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", "");
    }
}
