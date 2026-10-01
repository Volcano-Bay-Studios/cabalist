package xyz.volcanobay.cabalist.system.spell;

import net.minecraft.core.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.core.CabalistSpellDictionary;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.util.WordToNumberConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class SpellEngine {
    public static final SpellEngine INSTANCE = new SpellEngine();
    private static final char QUOTE = '"';
    private static final char QUOTED_SPACE = '~';
    public static final double MIN_CANDIDATE_SIMILARITY = 0.7;
    private static final String[] TOKENLESS = new String[0];
    private static final Set<String> GRAMMAR_WORDS = Set.of("when", "whenever", "if", "once", "while");

    public static boolean isGrammarWord(String token) {
        return GRAMMAR_WORDS.contains(token);
    }

    public CompletableFuture<SpellParse> parseFuture(String spell) {
        return CompletableFuture.supplyAsync(() -> parse(spell));
    }

    public SpellParse parse(String spell) {
        return selectSpans(tokenize(spell));
    }

    public Spell resolve(String text, Subject caster, double castSeconds) {
        return resolve(parse(text), caster, castSeconds);
    }

    public Spell resolve(SpellParse parse, Subject caster) {
        return resolve(parse, caster, parse.getEstimatedCastSeconds());
    }

    /**
     * Everything said before "inscribe" is the spell being inscribed
     */
    public Spell resolve(SpellParse parse, Subject caster, double castSeconds) {
        String[] tokens = parse.getTokens();
        int inscribeAt = getInscribeStart(parse);
        SpellParse cast = inscribeAt < 0 ? parse : parse.from(inscribeAt);
        Spell spell = new SpellResolver(caster, cast).resolve();
        spell.setCastSeconds(castSeconds);
        spell.setIncantation(join(tokens, 0, inscribeAt < 0 ? tokens.length : inscribeAt));
        return spell;
    }

    private static int getInscribeStart(SpellParse parse) {
        for (Word word : parse.getWords()) {
            if (word.getComponent() == CabalistSpellComponents.MATERIALIZE.get()) {
                return word.getStart();
            }
        }
        return -1;
    }

    /**
     * Tokenizes words with the only exception being that quotation marks hold phrases as one token.
     */
    public String[] tokenize(String spell) {
        StringBuilder marked = new StringBuilder();
        char[] characters = spell.toLowerCase().toCharArray();
        char open = 0;
        for (int i = 0; i < characters.length; i++) {
            char character = characters[i];
            boolean isWordStart = i == 0 || characters[i - 1] == ' ';
            boolean isWordEnd = i + 1 == characters.length || characters[i + 1] == ' ';
            if (open == 0 && (character == '"' || character == '\'' && isWordStart)) {
                marked.append(" " + QUOTE);
                open = character;
            } else if (open != 0 && character == open && (open == '"' || isWordEnd)) {
                marked.append(" ");
                open = 0;
            } else if (open != 0 && character == ' ') {
                marked.append(QUOTED_SPACE);
            } else {
                marked.append(character);
            }
        }
        String normalized = marked.toString().replaceAll("[^a-z0-9_ " + QUOTE + QUOTED_SPACE + "]", "").trim();
        if (normalized.isEmpty()) {
            return TOKENLESS;
        }
        String[] tokens = normalized.split(" +");
        List<String> kept = new ArrayList<>();
        for (String token : tokens) {
            if (!token.equals(String.valueOf(QUOTE))) {
                kept.add(token);
            }
        }
        return kept.toArray(String[]::new);
    }

    public static boolean isQuoted(String token) {
        return token.charAt(0) == QUOTE;
    }

    public static String getText(String token) {
        return isQuoted(token) ? token.substring(1).replace(QUOTED_SPACE, ' ') : token;
    }

    public static String join(String[] tokens, int from, int to) {
        StringBuilder text = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(isQuoted(tokens[i]) ? '"' + getText(tokens[i]) + '"' : tokens[i]);
        }
        return text.toString();
    }

    public SpellParse selectSpans(String[] tokens) {
        List<Candidate> candidates = new ArrayList<>();
        findNumberCandidates(tokens, candidates);
        findLiteralCandidates(tokens, candidates);
        Registry<SpellDictionary> dictionaries = CabalistSpellDictionary.getRegistry();
        if (dictionaries != null) {
            for (SpellDictionary dictionary : dictionaries) {
                if (dictionary.getSpellComponents().isEmpty()) {
                    continue;
                }
                findAllMatches(tokens, dictionary, MIN_CANDIDATE_SIMILARITY, candidates);
            }
        }
        return new SpellParse(tokens, chooseSpans(tokens.length, candidates));
    }

    public void findAllMatches(String[] tokens, SpellDictionary dictionary, double minSimilarity, List<Candidate> out) {
        int maxWords = dictionary.getMaxWordCount();
        StringBuilder window = new StringBuilder();
        for (int start = 0; start < tokens.length; start++) {
            window.setLength(0);
            int maxEnd = Math.min(tokens.length, start + maxWords);
            for (int end = start + 1; end <= maxEnd; end++) {
                if (end > start + 1) {
                    window.append(' ');
                }
                if (isQuoted(tokens[end - 1])) {
                    break;
                }
                window.append(tokens[end - 1]);
                SpellDictionary.Match match = dictionary.getMatch(window.toString());
                if (match.phrase() == null || coversGrammarWord(tokens, start, end, match.phrase())) {
                    continue;
                }
                boolean isPhrase = end - start > 1;
                boolean isTooWeak = isPhrase ? minSimilarity > 0 && match.score() <= 0 : match.similarity() < minSimilarity;
                if (isTooWeak) {
                    continue;
                }
                double score = match.score() * dictionary.getWeight(match.phrase());
                out.add(new Candidate(start, end, match.phrase(), match.similarity(), score, dictionary.getSpellComponents(), Double.NaN));
            }
        }
    }

    private static boolean coversGrammarWord(String[] tokens, int start, int end, String phrase) {
        for (int i = start; i < end; i++) {
            if (isGrammarWord(tokens[i]) && !Arrays.asList(phrase.split(" ")).contains(tokens[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Quoted text is usually a literal point to a target, like a contract or player.
     */
    private void findLiteralCandidates(String[] tokens, List<Candidate> out) {
        for (int i = 0; i < tokens.length; i++) {
            if (isQuoted(tokens[i])) {
                out.add(new Candidate(i, i + 1, getText(tokens[i]), 1.0, SpellDictionary.getExactScore(1), List.of(CabalistSpellComponents.LITERAL.get()), Double.NaN));
            }
        }
    }

    private void findNumberCandidates(String[] tokens, List<Candidate> out) {
        int start = 0;
        while (start < tokens.length) {
            int end = findNumberEnd(tokens, start);
            if (end == start) {
                start++;
                continue;
            }
            String phrase = String.join(" ", Arrays.copyOfRange(tokens, start, end));
            double number = WordToNumberConverter.convertPhraseToDouble(phrase);
            out.add(new Candidate(start, end, phrase, 1.0, SpellDictionary.getExactScore(end - start), List.of(CabalistSpellComponents.NUMBER.get()), number));
            start = end;
        }
    }

    /**
     * Allows "a hundred" and "one hundred and five", but not "five and six".
     */
    private int findNumberEnd(String[] tokens, int start) {
        int end = start;
        boolean hasNumber = false;
        while (end < tokens.length) {
            String token = tokens[end];
            if (WordToNumberConverter.isNumberWord(token)) {
                hasNumber = true;
                end++;
            } else if (token.equals("a") && !hasNumber && isMultiplierAt(tokens, end + 1)) {
                end++;
            } else if (token.equals("and") && hasNumber && isMultiplierAt(tokens, end - 1) && isNumberAt(tokens, end + 1)) {
                end++;
            } else {
                break;
            }
        }
        if (!hasNumber) {
            return start;
        }
        return end;
    }

    private static boolean isMultiplierAt(String[] tokens, int index) {
        return index >= 0 && index < tokens.length && WordToNumberConverter.isMultiplierWord(tokens[index]);
    }

    private static boolean isNumberAt(String[] tokens, int index) {
        return index >= 0 && index < tokens.length && WordToNumberConverter.isNumberWord(tokens[index]);
    }

    /**
     * picks the non-overlapping spans with the highest total score
     */
    private List<Candidate> chooseSpans(int tokenCount, List<Candidate> candidates) {
        Collections.sort(candidates);
        double[] bestScore = new double[tokenCount + 1];
        int[] chosenCandidate = new int[tokenCount + 1];
        chosenCandidate[0] = -1;
        int next = 0;
        for (int end = 1; end <= tokenCount; end++) {
            bestScore[end] = bestScore[end - 1];
            chosenCandidate[end] = -1;
            while (next < candidates.size() && candidates.get(next).end() == end) {
                Candidate candidate = candidates.get(next);
                double score = bestScore[candidate.start()] + candidate.score();
                if (score > bestScore[end]) {
                    bestScore[end] = score;
                    chosenCandidate[end] = next;
                }
                next++;
            }
        }

        List<Candidate> spans = new ArrayList<>();
        int end = tokenCount;
        while (end > 0) {
            if (chosenCandidate[end] == -1) {
                end--;
            } else {
                Candidate candidate = candidates.get(chosenCandidate[end]);
                spans.add(candidate);
                end = candidate.start();
            }
        }
        Collections.reverse(spans);
        return spans;
    }

    /**
     * A possible span of tokens.
     */
    public record Candidate(int start, int end, @Nullable String phrase, double similarity, double score,
                            List<SpellComponent> components, double number) implements Comparable<Candidate> {

        public boolean isNumber() {
            return !Double.isNaN(number);
        }

        @Override
        public int compareTo(@NotNull Candidate other) {
            return Integer.compare(end, other.end);
        }
    }
}
