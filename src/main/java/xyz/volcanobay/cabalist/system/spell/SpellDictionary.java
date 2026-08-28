package xyz.volcanobay.cabalist.system.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import edu.uniba.di.lacam.kdde.ws4j.WS4J;

import java.util.List;

public class SpellDictionary {
    public final List<String> words;

    public SpellDictionary(List<String> words) {
        this.words = words;
    }
    public static final Codec<SpellDictionary> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.list(Codec.STRING).fieldOf("words").forGetter(SpellDictionary::getWords)
    ).apply(instance, SpellDictionary::new));

    public List<String> getWords() {
        return words;
    }

    public double getSimilarity(String word) {
        double similarity = 0;
        for (String s : words) {
            similarity = Math.max(WS4J.runHSO(word, s), similarity);
        }
        return similarity;
    }
}
