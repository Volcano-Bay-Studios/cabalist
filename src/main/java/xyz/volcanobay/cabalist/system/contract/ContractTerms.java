package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.UsernameCache;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.content.spell.component.AspectComponent;
import xyz.volcanobay.cabalist.core.CabalistSpellDictionary;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellDictionary;
import xyz.volcanobay.cabalist.system.spell.SpellEngine;
import xyz.volcanobay.cabalist.util.WordToNumberConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ContractTerms {
    private static final double TERM_SIMILARITY = 0.85;
    private static final Set<String> FILLER = Set.of("of", "the", "a", "an", "is", "are", "and", "must", "percent", "all", "to");
    private static final Map<String, Double> FRACTIONS = Map.of("half", 0.5, "third", 1 / 3.0, "quarter", 0.25, "majority", 0.51, "all", 1.0);

    public record AmendRules(double threshold, @Nullable Set<UUID> voterGroup, Set<UUID> required, Set<UUID> exempt) {
        public Set<UUID> getVoters(Contract contract) {
            Set<UUID> voters = new HashSet<>();
            voters.addAll(voterGroup != null ? voterGroup : contract.getContracteeIds());
            voters.removeAll(exempt);
            return voters;
        }
    }

    public static boolean isTerm(String line) {
        if (!match(SpellEngine.INSTANCE.tokenize(line), Kind.LIMIT).isEmpty()) {
            return true;
        }
        for (SpellEngine.Candidate span : SpellEngine.INSTANCE.parse(line).getSpans()) {
            for (SpellComponent component : span.components()) {
                if (component instanceof AspectComponent) {
                    return false;
                }
            }
        }
        String[] tokens = SpellEngine.INSTANCE.tokenize(line);
        for (Kind kind : Kind.values()) {
            if (!match(tokens, kind).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAuthorizing(List<String> lines) {
        return hasKind(lines, Kind.AUTHORIZATION);
    }

    private static boolean hasKind(List<String> lines, Kind kind) {
        for (String line : lines) {
            if (!match(SpellEngine.INSTANCE.tokenize(line), kind).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasKindEverywhere(List<String> lines, Kind kind) {
        for (String line : lines) {
            String[] tokens = SpellEngine.INSTANCE.tokenize(line);
            if (!match(tokens, kind).isEmpty() && !match(tokens, Kind.EVERYWHERE).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public record Limit(String tag, int count) {
    }

    public record Settings(boolean isAuthorizing, boolean isAuthorizingEverywhere, boolean isMembersPay, boolean isMembersPayEverywhere,
                           double radius, Set<String> tags, List<Limit> limits) {
    }

    public static Settings readSettings(Contract contract) {
        List<String> lines = contract.getIncantations();
        return new Settings(hasKind(lines, Kind.AUTHORIZATION), hasKindEverywhere(lines, Kind.AUTHORIZATION), hasKind(lines, Kind.MEMBERS_PAY),
                hasKindEverywhere(lines, Kind.MEMBERS_PAY), getRadius(lines), getTags(lines), getLimits(lines));
    }

    private static Set<String> getTags(List<String> lines) {
        Set<String> tags = new HashSet<>();
        for (String line : lines) {
            String[] tokens = SpellEngine.INSTANCE.tokenize(line);
            if (isTerm(line) && match(tokens, Kind.LIMIT).isEmpty() && !match(tokens, Kind.TAGGED).isEmpty()) {
                String tag = readLiteral(tokens);
                if (tag != null) {
                    tags.add(tag);
                }
            }
        }
        return tags;
    }

    private static List<Limit> getLimits(List<String> lines) {
        List<Limit> limits = new ArrayList<>();
        for (String line : lines) {
            String[] tokens = SpellEngine.INSTANCE.tokenize(line);
            if (match(tokens, Kind.LIMIT).isEmpty()) {
                continue;
            }
            String tag = readLiteral(tokens);
            double count = readNumber(tokens);
            if (tag != null && !Double.isNaN(count)) {
                limits.add(new Limit(tag, (int) count));
            }
        }
        return limits;
    }

    private static double getRadius(List<String> lines) {
        for (String line : lines) {
            String[] tokens = SpellEngine.INSTANCE.tokenize(line);
            if (isTerm(line) && !match(tokens, Kind.RADIUS).isEmpty()) {
                double radius = readNumber(tokens);
                if (!Double.isNaN(radius)) {
                    return radius;
                }
            }
        }
        return Double.NaN;
    }

    private static @Nullable String readLiteral(String[] tokens) {
        for (String token : tokens) {
            if (SpellEngine.isQuoted(token)) {
                return SpellEngine.getText(token).trim().toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    private static double readNumber(String[] tokens) {
        int start = 0;
        while (start < tokens.length && !WordToNumberConverter.isNumberWord(tokens[start])) {
            start++;
        }
        int end = start;
        while (end < tokens.length && (WordToNumberConverter.isNumberWord(tokens[end]) || tokens[end].equals("and") && end + 1 < tokens.length
                && WordToNumberConverter.isNumberWord(tokens[end + 1]))) {
            end++;
        }
        return start == end ? Double.NaN : WordToNumberConverter.convertPhraseToDouble(String.join(" ", Arrays.copyOfRange(tokens, start, end)));
    }

    public static AmendRules read(Contract contract, MinecraftServer server) {
        double threshold = 1;
        Set<UUID> voterGroup = null;
        Set<UUID> required = new HashSet<>();
        Set<UUID> exempt = new HashSet<>();
        for (String line : contract.getIncantations()) {
            String[] tokens = SpellEngine.INSTANCE.tokenize(line);
            if (isSetting(tokens)) {
                continue;
            }
            if (!match(tokens, Kind.THRESHOLD).isEmpty()) {
                double value = readFraction(tokens);
                if (!Double.isNaN(value)) {
                    threshold = value;
                }
            } else if (!match(tokens, Kind.REQUIRED).isEmpty()) {
                required.addAll(readSubjects(tokens, contract, server));
            } else if (!match(tokens, Kind.EXEMPT).isEmpty()) {
                exempt.addAll(readSubjects(tokens, contract, server));
            } else if (!match(tokens, Kind.MEMBERS).isEmpty()) {
                if (voterGroup == null) {
                    voterGroup = new HashSet<>();
                }
                voterGroup.addAll(readSubjects(tokens, contract, server));
            }
        }
        return new AmendRules(threshold, voterGroup, required, exempt);
    }

    private static boolean isSetting(String[] tokens) {
        return !match(tokens, Kind.LIMIT).isEmpty() || !match(tokens, Kind.TAGGED).isEmpty() || !match(tokens, Kind.RADIUS).isEmpty();
    }

    private static List<SpellEngine.Candidate> match(String[] tokens, Kind kind) {
        List<SpellEngine.Candidate> matches = new ArrayList<>();
        SpellDictionary dictionary = CabalistSpellDictionary.getSpellDictionary(Cabalist.id(kind.dictionary));
        if (dictionary != null) {
            SpellEngine.INSTANCE.findAllMatches(tokens, dictionary, TERM_SIMILARITY, matches);
            matches.removeIf(match -> match.similarity() < TERM_SIMILARITY);
        }
        return matches;
    }

    private static double readFraction(String[] tokens) {
        for (String token : tokens) {
            Double fraction = FRACTIONS.get(token);
            if (fraction != null) {
                return fraction;
            }
        }
        double number = WordToNumberConverter.convertPhraseToDouble(String.join(" ", tokens));
        if (Double.isNaN(number)) {
            return Double.NaN;
        }
        return Math.min(1, number > 1 ? number / 100 : number);
    }

    private static Set<UUID> readSubjects(String[] tokens, Contract reader, MinecraftServer server) {
        boolean[] isCovered = new boolean[tokens.length];
        for (Kind kind : Kind.values()) {
            for (SpellEngine.Candidate match : match(tokens, kind)) {
                for (int i = match.start(); i < match.end(); i++) {
                    isCovered[i] = true;
                }
            }
        }
        Set<UUID> subjects = new HashSet<>();
        for (int i = 0; i < tokens.length; i++) {
            if (SpellEngine.isQuoted(tokens[i])) {
                addQuoted(SpellEngine.getText(tokens[i]), reader, server, subjects);
                isCovered[i] = true;
            }
            isCovered[i] |= FILLER.contains(tokens[i]) || WordToNumberConverter.isNumberWord(tokens[i]);
        }
        int start = 0;
        while (start < tokens.length) {
            if (isCovered[start]) {
                start++;
                continue;
            }
            int end = start;
            while (end < tokens.length && !isCovered[end]) {
                end++;
            }
            for (int i = start; i < end; i++) {
                UUID player = findPlayer(tokens[i], server);
                if (player != null) {
                    subjects.add(player);
                }
            }
            start = end;
        }
        return subjects;
    }

    private static void addQuoted(String name, Contract reader, MinecraftServer server, Set<UUID> out) {
        Contract contract = findContract(name, reader);
        if (contract != null) {
            out.addAll(contract.getContracteeIds());
            return;
        }
        UUID player = findPlayer(name, server);
        if (player != null) {
            out.add(player);
        }
    }

    private static @Nullable Contract findContract(String name, Contract reader) {
        for (Contract contract : ContractSystem.INSTANCE.getContracts()) {
            if (contract.getName().equalsIgnoreCase(name) && contract.canBeNamedBy(reader)) {
                return contract;
            }
        }
        return null;
    }

    private static @Nullable UUID findPlayer(String name, MinecraftServer server) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            return online.getUUID();
        }
        for (Map.Entry<UUID, String> known : UsernameCache.getMap().entrySet()) {
            if (known.getValue().equalsIgnoreCase(name)) {
                return known.getKey();
            }
        }
        return null;
    }

    private enum Kind {
        THRESHOLD("term_threshold"),
        REQUIRED("term_required"),
        EXEMPT("term_exempt"),
        MEMBERS("term_members"),
        AUTHORIZATION("term_authorization"),
        MEMBERS_PAY("term_members_pay"),
        TAGGED("term_tagged"),
        LIMIT("term_limit"),
        RADIUS("term_radius"),
        EVERYWHERE("term_everywhere");

        private final String dictionary;

        Kind(String dictionary) {
            this.dictionary = dictionary;
        }
    }
}
