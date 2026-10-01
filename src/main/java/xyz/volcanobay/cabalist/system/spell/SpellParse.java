package xyz.volcanobay.cabalist.system.spell;

import xyz.volcanobay.cabalist.core.CabalistConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The spans chosen from one utterance. Tokens not covered by a span are devotional, except grammar words.
 */
public class SpellParse {
    private final String[] tokens;
    private final List<SpellEngine.Candidate> spans;
    private final boolean[] devotional;
    private final List<Word> words = new ArrayList<>();

    public SpellParse(String[] tokens, List<SpellEngine.Candidate> spans) {
        this.tokens = tokens;
        this.spans = spans;
        this.devotional = new boolean[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            devotional[i] = !SpellEngine.isGrammarWord(tokens[i]) && !SpellEngine.isQuoted(tokens[i]);
        }
        int claimedFrom = tokens.length;
        for (SpellEngine.Candidate span : spans) {
            if (span.start() >= claimedFrom) {
                break;
            }
            for (int i = span.start(); i < span.end(); i++) {
                devotional[i] = false;
            }
            for (SpellComponent component : span.components()) {
                Word word = new Word(component, span.start(), span.end(), span.score(), span.number());
                if (span.phrase() != null) {
                    word.setPhrase(span.phrase());
                }
                words.add(word);
                if (component.claimsTrailingText()) {
                    claimedFrom = Math.min(claimedFrom, span.end());
                }
            }
        }
        for (int i = claimedFrom; i < tokens.length; i++) {
            devotional[i] = false;
        }
        for (Word word : words) {
            boolean isClaiming = word.getComponent().claimsTrailingText();
            word.setTrailingText(isClaiming ? getTextAfter(word.getEnd()) : getDevotionalTextAfter(word.getEnd()));
        }
    }

    private String getTextAfter(int start) {
        StringBuilder text = new StringBuilder();
        for (int i = start; i < tokens.length; i++) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(SpellEngine.getText(tokens[i]));
        }
        return text.toString();
    }

    private String getDevotionalTextAfter(int start) {
        int first = start;
        while (first < tokens.length && !devotional[first]) {
            first++;
        }
        while (first < tokens.length && devotional[first] && (tokens[first].equals("to") || tokens[first].equals("as"))) {
            first++;
        }
        StringBuilder text = new StringBuilder();
        for (int i = first; i < tokens.length && devotional[i]; i++) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(tokens[i]);
        }
        return text.toString();
    }

    public SpellParse from(int start) {
        List<SpellEngine.Candidate> kept = new ArrayList<>();
        for (SpellEngine.Candidate span : spans) {
            if (span.start() >= start) {
                kept.add(new SpellEngine.Candidate(span.start() - start, span.end() - start, span.phrase(), span.similarity(), span.score(), span.components(), span.number()));
            }
        }
        return new SpellParse(Arrays.copyOfRange(tokens, start, tokens.length), kept);
    }

    public double getEstimatedCastSeconds() {
        return tokens.length * CabalistConfig.SECONDS_PER_SPOKEN_WORD.get();
    }

    public static double estimateCastSeconds(String incantation) {
        return SpellEngine.INSTANCE.tokenize(incantation).length * CabalistConfig.SECONDS_PER_SPOKEN_WORD.get();
    }

    public List<Word> getWords() {
        return words;
    }

    public String[] getTokens() {
        return tokens;
    }

    public List<SpellEngine.Candidate> getSpans() {
        return spans;
    }

    public boolean isDevotional(int token) {
        return devotional[token];
    }

    public int getDevotionalCount() {
        int count = 0;
        for (boolean isDevotional : devotional) {
            if (isDevotional) {
                count++;
            }
        }
        return count;
    }
}
