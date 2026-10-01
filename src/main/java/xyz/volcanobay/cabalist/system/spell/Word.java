package xyz.volcanobay.cabalist.system.spell;

import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.system.subject.Subject;

/**
 * One component spoken in one cast. A word is also the value other words claim.
 */
public class Word {
    private final SpellComponent component;
    private final int start;
    private final int end;
    private final double score;
    private final double number;
    private final Word[] claimed;
    private Word claimedBy;
    private boolean isResolved;
    private SpellClause clause;
    private @Nullable Subject subject;
    private double measure = Double.NaN;
    private boolean joinsRequirements;
    private String trailingText = "";
    private String phrase = "";

    public Word(SpellComponent component, int start, int end, double score, double number) {
        this.component = component;
        this.start = start;
        this.end = end;
        this.score = score;
        this.number = number;
        this.claimed = new Word[component.getNeeds().length];
    }

    public SpellComponent getComponent() {
        return component;
    }

    public SpellRole getRole() {
        return component.getRole();
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public double getScore() {
        return score;
    }

    public double getStrength() {
        return score / SpellDictionary.getExactScore(end - start);
    }

    /**
     * NaN unless this word is a number.
     */
    public double getNumber() {
        return number;
    }

    public boolean hasNumber() {
        return !Double.isNaN(number);
    }

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public String getTrailingText() {
        return trailingText;
    }

    public void setTrailingText(String trailingText) {
        this.trailingText = trailingText;
    }

    public boolean joinsRequirements() {
        return joinsRequirements;
    }

    public void setJoinsRequirements(boolean joinsRequirements) {
        this.joinsRequirements = joinsRequirements;
    }

    public double getMeasure() {
        return measure;
    }

    public void setMeasure(double measure) {
        this.measure = measure;
    }

    public SpellClause getClause() {
        return clause;
    }

    public void setClause(SpellClause clause) {
        this.clause = clause;
    }

    public @Nullable Subject getSubject() {
        return subject;
    }

    public void setSubject(@Nullable Subject subject) {
        this.subject = subject;
    }

    public int getNeedCount() {
        return claimed.length;
    }

    public @Nullable Word getClaimed(int needIndex) {
        return claimed[needIndex];
    }

    public @Nullable Word getClaimed(SpellRole role) {
        SpellRole[] needs = component.getNeeds();
        for (int i = 0; i < claimed.length; i++) {
            if (needs[i] == role && claimed[i] != null) {
                return claimed[i];
            }
        }
        return null;
    }

    public boolean hasAllNeeds() {
        for (Word word : claimed) {
            if (word == null) {
                return false;
            }
        }
        return true;
    }

    public void claim(int needIndex, Word word) {
        claimed[needIndex] = word;
        word.claimedBy = this;
    }

    public @Nullable Word getClaimedBy() {
        return claimedBy;
    }

    public boolean isClaimed() {
        return claimedBy != null;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void markResolved() {
        isResolved = true;
    }

    public int distanceTo(Word other) {
        if (other.start >= end) {
            return other.start - end;
        }
        if (start >= other.end) {
            return start - other.end;
        }
        return 0;
    }
}
