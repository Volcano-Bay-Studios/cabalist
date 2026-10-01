package xyz.volcanobay.cabalist.system.spell;

import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * A clause's devotion. Generic devotion helps every aspect. Devotional phrases only help that domain, and hurt others.
 */
public class Devotion {
    private final Map<Domain, Double> phrases = new HashMap<>();
    private final Map<Domain, Double> domainWords = new HashMap<>();
    private final Map<String, Integer> uses = new HashMap<>();
    private int genericWords;

    public void addGenericWords(int words) {
        genericWords += words;
    }

    public void addPhrase(Domain domain, String phrase, double strength) {
        phrases.merge(domain, strength * getRepeatFactor("phrase:" + phrase), Double::sum);
    }

    public void addDomainWord(Domain domain, String phrase, double strength) {
        domainWords.merge(domain, strength * getRepeatFactor("domain:" + phrase), Double::sum);
    }

    private double getRepeatFactor(String key) {
        int previousUses = uses.merge(key, 1, Integer::sum) - 1;
        return Math.pow(CabalistConfig.DEVOTION_REPEAT_FALLOFF.get(), previousUses);
    }

    public int getGenericWords() {
        return genericWords;
    }

    public Map<Domain, Double> getPhrases() {
        return phrases;
    }

    public Map<Domain, Double> getDomainWords() {
        return domainWords;
    }

    public double getValue(@Nullable Domain domain) {
        double raw = genericWords * CabalistConfig.DEVOTION_GENERIC_RATE.get();
        if (domain != null) {
            raw += CabalistConfig.DEVOTION_PHRASE_RATE.get() * getAlignment(phrases, domain);
        }
        return saturate(raw);
    }

    public double getStrengthMultiplier(@Nullable Domain domain) {
        return 1 + CabalistConfig.DEVOTION_STRENGTH_RANGE.get() * getValue(domain);
    }

    public double getCostMultiplier(@Nullable Domain domain) {
        return (1 - CabalistConfig.DEVOTION_COST_RANGE.get() * getValue(domain)) / getStrengthMultiplier(domain);
    }

    public double getSizeMultiplier(@Nullable Domain domain) {
        return 1 + CabalistConfig.DEVOTION_SIZE_RANGE.get() * getValue(domain);
    }

    public double getCapacityMultiplier(@Nullable Domain domain) {
        if (domain == null) {
            return 1;
        }
        return 1 + CabalistConfig.DEVOTION_CAPACITY_RANGE.get() * saturate(CabalistConfig.DEVOTION_DOMAIN_WORD_RATE.get() * getAlignment(domainWords, domain));
    }

    private static double getAlignment(Map<Domain, Double> strengths, Domain domain) {
        double alignment = 0;
        for (Map.Entry<Domain, Double> entry : strengths.entrySet()) {
            alignment += entry.getKey() == domain ? entry.getValue() : -entry.getValue();
        }
        return alignment;
    }

    private static double saturate(double value) {
        return Math.signum(value) * (1 - Math.exp(-Math.abs(value)));
    }
}
