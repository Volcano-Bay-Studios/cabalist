package xyz.volcanobay.cabalist.system.spell;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

public class SpellEngine {
    public static final SpellEngine INSTANCE = new SpellEngine();

    public CompletableFuture<Spell> parseSpellFuture(String spell) {
        return CompletableFuture.supplyAsync(() -> parseSpell(spell));
    }

    public Spell parseSpell(String spell) {
        // tokenize
        spell = spell.toLowerCase();
        spell = spell.replaceAll("[^a-zA-Z0-9 ]", "");
        String[] tokens = spell.split(" ");

        

        return new Spell();
    }

    public TreeMap<String, Double> findAllMatches(String[] tokens, SpellDictionary dictionary) {
        TreeMap<String, Double> results = new TreeMap<>();

        int maxWords = Math.min(dictionary.getMaxWordCount(), tokens.length);
        for (int windowSize = 1; windowSize <= maxWords; windowSize++) { // Window is a span of the word when split, used for multi-word dictionary entries/phrases
            for (int i = 0; i <= tokens.length - windowSize; i++) {
                String window = String.join(" ", Arrays.copyOfRange(tokens, i, i + windowSize));
                double score = dictionary.getSimilarity(window);
                if (score > results.getOrDefault(window, -1d)) {
                    results.put(window, score);
                }
            }
        }
        return results;
    }

    public record Candidate(int start, int end, List<SpellComponent> components, double score) {

    }
}