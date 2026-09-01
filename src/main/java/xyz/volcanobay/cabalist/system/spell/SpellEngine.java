package xyz.volcanobay.cabalist.system.spell;

import edu.uniba.di.lacam.kdde.ws4j.WS4J;
import xyz.volcanobay.cabalist.core.CabalistDomains;
import xyz.volcanobay.voicelib.api.util.PhoneticComparison;

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


    public TreeMap<String,Double> findAllMatches(String[] tokens, SpellDictionary dictionary) {
        TreeMap<String,Double> results = new TreeMap<>();

        for (String word : dictionary.getWords()) {
            TreeMap<String,Double> matches = findMatches(tokens, word);
            for (String key : matches.keySet()) {
                Double value = matches.get(key);
                if (value > results.getOrDefault(key,-1d)) {
                    results.put(key,value);
                }
            }
        }
        return results;
    }

    public TreeMap<String,Double> findMatches(String[] tokens, String searchTerm) {
        searchTerm = searchTerm.toLowerCase();
        searchTerm = searchTerm.replaceAll("[^a-zA-Z0-9 ]", "");
        String[] searchTokens = searchTerm.split(" ");

        TreeMap<String,Double> results = new TreeMap<>();

        for (int i = 0; i < tokens.length - (searchTokens.length - 1); i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tokens[i]);
            for (int j = 1; j < searchTokens.length; j++) {
                stringBuilder.append(" ").append(searchTokens[i+j]);
            }
            String compareTerm = stringBuilder.toString();

            double phonetic = PhoneticComparison.calculate(compareTerm, searchTerm);
            double semantic = WS4J.runHSO(compareTerm, searchTerm)/16d;
            double finalResult = Math.max(phonetic,semantic);

            results.put(compareTerm,finalResult);
        }
        
        return results;
    }
}
