package xyz.volcanobay.cabalist.system.spell;

import xyz.volcanobay.cabalist.core.CabalistDomains;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SpellEngine {
    public static final SpellEngine INSTANCE = new SpellEngine();
    private static final List<CompletableFuture<Spell>> spellFutures = new ArrayList<>();

    public void parseSpellFuture(String spell) {
        CompletableFuture<Spell> spellFuture = new CompletableFuture<>();
        spellFuture.completeAsync(() -> parseSpell(spell));
        spellFutures.add(spellFuture);
    }

    public Spell parseSpell(String spell) {
        // tokenize
        spell = spell.toLowerCase();
        spell = spell.replaceAll("[^a-zA-Z0-9 ]", "");
        String[] tokens = spell.split(" ");

        // find domain candidates


        // find subject candidates


        // find source candidates


        // find modifier candidates


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
}