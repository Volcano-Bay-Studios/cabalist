package xyz.volcanobay.cabalist.system.spell;

import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.content.spell.referent.ThisReferent;
import xyz.volcanobay.cabalist.core.CabalistAspects;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.form.FormDimension;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Must run on the server thread, since components may read world state.
 * Words resolve in priority, each need claims the nearest resolved, unclaimed word in the same clause.
 */
public class SpellResolver {
    private static final double SPARE_NUMBER_POWER = 0.1;

    private final Subject caster;
    private final SpellParse parse;
    private final List<Word> words;
    private final List<SpellClause> clauses = new ArrayList<>();

    public SpellResolver(Subject caster, SpellParse parse) {
        this.caster = caster;
        this.parse = parse;
        this.words = parse.getWords();
    }

    public Subject getCaster() {
        return caster;
    }

    public String getSpokenText(Word word) {
        String[] tokens = parse.getTokens();
        return String.join(" ", Arrays.copyOfRange(tokens, word.getStart(), Math.min(word.getEnd(), tokens.length)));
    }

    public Spell resolve() {
        assignClauses();
        assignGenericDevotion();
        for (Word word : words) {
            if (word.getNeedCount() == 0) {
                resolveWord(word);
            }
        }

        Word[] ordered = sortUnresolvedByPriority();
        int tierStart = 0;
        while (tierStart < ordered.length) {
            int priority = ordered[tierStart].getComponent().getPriority();
            int tierEnd = tierStart;
            while (tierEnd < ordered.length && ordered[tierEnd].getComponent().getPriority() == priority) {
                tierEnd++;
            }
            resolveTier(ordered, tierStart, tierEnd);
            tierStart = tierEnd;
        }

        routeLeftovers();
        double resonance = getResonance();
        for (SpellClause clause : clauses) {
            clause.inheritFromPrevious();
            if (clause.getAspects().isEmpty() && clause.isReleased()) {
                clause.addAspect(CabalistAspects.RELEASE.get());
            }
            if (clause.getAspects().isEmpty() && !clause.getColors().isEmpty()) {
                clause.addAspect(CabalistAspects.RECOLOR.get());
            }
            aimWhereLooking(clause);
            clause.setPower((float) (resonance + clause.getSpareNumber() * SPARE_NUMBER_POWER));
        }
        return new Spell(clauses, caster);
    }

    private double getResonance() {
        double totalScore = 0;
        int scoredWords = 0;
        for (Word word : words) {
            SpellRole role = word.getRole();
            if (role == SpellRole.CONJUNCTION || role == SpellRole.DEVOTIONAL || role == SpellRole.INVOCATION) {
                continue;
            }
            totalScore += word.getStrength();
            scoredWords++;
        }
        if (scoredWords == 0) {
            return 0;
        }
        return totalScore / scoredWords;
    }

    private void assignGenericDevotion() {
        int wordIndex = 0;
        SpellClause clause = clauses.get(0);
        for (int token = 0; token < parse.getTokens().length; token++) {
            while (wordIndex < words.size() && words.get(wordIndex).getStart() <= token) {
                clause = words.get(wordIndex).getClause();
                wordIndex++;
            }
            if (parse.isDevotional(token)) {
                clause.getDevotion().addGenericWords(1);
            }
        }
    }

    private void assignClauses() {
        SpellClause clause = new SpellClause(caster);
        clauses.add(clause);
        boolean clauseHasWords = false;
        for (int i = 0; i < words.size(); i++) {
            Word word = words.get(i);
            if (word.getRole() == SpellRole.CONJUNCTION) {
                word.setJoinsRequirements(isRequirementAt(i - 1) && isRequirementAt(i + 1));
                if (!word.joinsRequirements() && word.getComponent().splitsClauses() && clauseHasWords) {
                    clause = new SpellClause(clause);
                    clauses.add(clause);
                }
            }
            word.setClause(clause);
            clauseHasWords = true;
        }
    }

    private boolean isRequirementAt(int index) {
        return index >= 0 && index < words.size() && words.get(index).getRole() == SpellRole.CONDITION;
    }

    private Word[] sortUnresolvedByPriority() {
        int count = 0;
        for (Word word : words) {
            if (!word.isResolved()) {
                count++;
            }
        }
        Word[] ordered = new Word[count];
        int size = 0;
        for (Word word : words) {
            if (word.isResolved()) {
                continue;
            }
            int index = size;
            int priority = word.getComponent().getPriority();
            while (index > 0 && ordered[index - 1].getComponent().getPriority() < priority) {
                ordered[index] = ordered[index - 1];
                index--;
            }
            ordered[index] = word;
            size++;
        }
        return ordered;
    }

    private void resolveTier(Word[] ordered, int start, int end) {
        boolean madeProgress = true;
        while (madeProgress) {
            madeProgress = false;
            for (int i = start; i < end; i++) {
                Word word = ordered[i];
                if (word.isResolved()) {
                    continue;
                }
                claimNearest(word);
                if (word.hasAllNeeds()) {
                    resolveWord(word);
                    madeProgress = true;
                }
            }
        }
        for (int i = start; i < end; i++) {
            if (!ordered[i].isResolved()) {
                resolveWord(ordered[i]);
            }
        }
    }

    private void resolveWord(Word word) {
        word.getComponent().resolve(word, this);
        word.markResolved();
    }

    private void claimNearest(Word word) {
        SpellRole[] needs = word.getComponent().getNeeds();
        for (int need = 0; need < needs.length; need++) {
            if (word.getClaimed(need) != null) {
                continue;
            }
            Word nearest = findNearest(word, needs[need]);
            if (nearest != null) {
                word.claim(need, nearest);
            }
        }
    }

    private @Nullable Word findNearest(Word claimer, SpellRole role) {
        Word nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (Word candidate : words) {
            if (candidate == claimer || candidate.getRole() != role || !candidate.isResolved() || candidate.isClaimed()) {
                continue;
            }
            if (candidate.getClause() != claimer.getClause()) {
                continue;
            }
            int distance = claimer.distanceTo(candidate);
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private void aimWhereLooking(SpellClause clause) {
        if (clause.getTarget() != null || clause.getForm() != null || !(caster instanceof EntitySubject entitySubject)) {
            return;
        }
        for (Aspect aspect : clause.getAspects()) {
            if (aspect.aimsWhereLooking()) {
                Subject seen = ThisReferent.findLookedAt(entitySubject.getEntity());
                if (seen != null) {
                    clause.setTarget(seen);
                }
                return;
            }
        }
    }

    private void routeLeftovers() {
        for (Word word : words) {
            if (word.isClaimed()) {
                continue;
            }
            SpellClause clause = word.getClause();
            Subject subject = word.getSubject();
            boolean formNeedsSize = clause.getForm() != null && Double.isNaN(clause.getFormDimension(FormDimension.SIZE));
            if (formNeedsSize && !Double.isNaN(word.getMeasure())) {
                clause.setFormDimension(FormDimension.SIZE, word.getMeasure());
            } else if (subject != null) {
                if (clause.getTarget() == null) {
                    clause.setTarget(subject);
                } else if (clause.getOrigin() == null) {
                    clause.setOrigin(subject);
                }
            } else if (word.getRole() == SpellRole.NUMBER) {
                clause.addSpareNumber(word.getNumber());
            }
        }
    }
}
