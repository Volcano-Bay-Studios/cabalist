package xyz.volcanobay.cabalist.system.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.content.term.RequirementTerm;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistTerms;
import xyz.volcanobay.cabalist.system.contract.Term;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PendingSpell {
    private final Spell spell;
    private final Subject host;
    private final String incantation;
    private final double castSeconds;
    private final boolean repeats;
    private final @Nullable EnergyStack paymentStack;
    private final Term[][] requirements;
    private final int[][] requirementGroups;
    private final long[][] metAt;
    private final int[] fireCount;
    private final long[] lastAttempt;
    private final Subject[] lastCause;
    private final @Nullable Spell[] running;
    private boolean isDismissed;

    public PendingSpell(Spell spell, Subject host, boolean repeats, @Nullable EnergyStack paymentStack) {
        this.spell = spell;
        this.host = host;
        this.incantation = spell.getIncantation();
        this.castSeconds = spell.getCastSeconds();
        this.repeats = repeats;
        this.paymentStack = paymentStack;
        List<SpellClause> clauses = spell.getClauses();
        int clauseCount = clauses.size();
        requirements = new Term[clauseCount][];
        requirementGroups = new int[clauseCount][];
        metAt = new long[clauseCount][];
        fireCount = new int[clauseCount];
        lastAttempt = new long[clauseCount];
        lastCause = new Subject[clauseCount];
        running = new Spell[clauseCount];
        for (int i = 0; i < clauseCount; i++) {
            List<List<Term>> groups = clauses.get(i).getRequirementGroups();
            int count = 0;
            for (List<Term> group : groups) {
                count += group.size();
            }
            requirements[i] = new Term[count];
            requirementGroups[i] = new int[count];
            metAt[i] = new long[count];
            int index = 0;
            for (int group = 0; group < groups.size(); group++) {
                for (Term term : groups.get(group)) {
                    requirements[i][index] = term;
                    requirementGroups[i][index] = group;
                    metAt[i][index] = -1;
                    index++;
                }
            }
            lastAttempt[i] = Long.MIN_VALUE / 2;
        }
    }

    public static PendingSpell running(Spell spell, Subject host) {
        PendingSpell pending = new PendingSpell(spell, host, false, null);
        for (int i = 0; i < pending.fireCount.length; i++) {
            pending.fireCount[i] = 1;
            pending.running[i] = spell;
        }
        return pending;
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putString("incantation", incantation);
        tag.putDouble("cast_seconds", castSeconds);
        tag.putIntArray("fire_count", fireCount);
        tag.putIntArray("colors", spell.getColors().stream().mapToInt(Integer::intValue).toArray());
        tag.put("energy", spell.getEnergyStack().write(new CompoundTag()));
        return tag;
    }

    public static PendingSpell read(CompoundTag tag, Subject host) {
        String incantation = tag.getString("incantation");
        double castSeconds = tag.getDouble("cast_seconds");
        Spell spell = SpellEngine.INSTANCE.resolve(incantation, host, castSeconds);
        spell.setIncantation(incantation);
        spell.getEnergyStack().read(tag.getCompound("energy"));
        spell.setColors(Arrays.stream(tag.getIntArray("colors")).boxed().toList());
        PendingSpell pending = new PendingSpell(spell, host, false, spell.getEnergyStack());
        int[] savedFireCount = tag.getIntArray("fire_count");
        System.arraycopy(savedFireCount, 0, pending.fireCount, 0, Math.min(savedFireCount.length, pending.fireCount.length));
        return pending;
    }

    public Spell getSpell() {
        return spell;
    }

    public Subject getHost() {
        return host;
    }

    public String getIncantation() {
        return incantation;
    }

    public int getClauseCount() {
        return fireCount.length;
    }

    public int getFireCount(int clause) {
        return fireCount[clause];
    }

    public List<Spell> getRunningSpells() {
        List<Spell> spells = new ArrayList<>();
        for (int i = 0; i < running.length; i++) {
            if (isRunning(i) && !spells.contains(running[i])) {
                spells.add(running[i]);
            }
        }
        return spells;
    }

    public boolean isRunning(int clause) {
        Spell spell = running[clause];
        return spell != null && spell.isRunning() && !spell.isDismissed();
    }

    public boolean isRunning() {
        for (int i = 0; i < running.length; i++) {
            if (isRunning(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This is the text that causes the spell to await. The text is the condition being waited upon.
     */
    public String getAwaitingText() {
        List<String> groups = new ArrayList<>();
        List<SpellClause> clauses = spell.getClauses();
        for (int clause = 0; clause < requirements.length && clause < clauses.size(); clause++) {
            if (isDismissed || isRunning(clause) || (!repeats && fireCount[clause] > 0)) {
                continue;
            }
            for (List<String> group : clauses.get(clause).getRequirementPhrases()) {
                String text = String.join(" and ", group);
                if (!text.isBlank() && !groups.contains(text)) {
                    groups.add(text);
                }
            }
        }
        return String.join(" or ", groups);
    }

    public boolean isAwaiting(ResourceLocation type) {
        if (isDismissed) {
            return false;
        }
        for (int clause = 0; clause < requirements.length; clause++) {
            if (!repeats && fireCount[clause] > 0) {
                continue;
            }
            for (Term term : requirements[clause]) {
                if (term.getResourceLocation().equals(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isWaiting() {
        if (isDismissed) {
            return false;
        }
        if (repeats) {
            return true;
        }
        for (int count : fireCount) {
            if (count == 0) {
                return true;
            }
        }
        return false;
    }

    public void dismiss() {
        isDismissed = true;
        for (Spell spell : running) {
            if (spell != null) {
                spell.dismiss();
            }
        }
    }

    public void release() {
        isDismissed = true;
        for (Spell spell : running) {
            if (spell != null && !spell.isReleased()) {
                spell.dismiss();
            }
        }
    }

    public void onTrigger(TriggerEvent event, long gameTime) {
        for (int i = 0; i < requirements.length; i++) {
            for (int j = 0; j < requirements[i].length; j++) {
                if (requirements[i][j].getResourceLocation().equals(event.type()) && requirements[i][j].isMetBy(event, host)) {
                    metAt[i][j] = gameTime;
                    lastCause[i] = event.cause();
                }
            }
        }
        fireReady(gameTime);
    }

    public int fireReady(long gameTime) {
        int affected = 0;
        for (int i = 0; i < fireCount.length; i++) {
            if (isReady(i, gameTime)) {
                affected += Math.max(0, fire(i, gameTime));
            }
        }
        return affected;
    }

    public boolean isComplete() {
        return !isWaiting() && !isRunning();
    }

    private boolean isReady(int clause, long gameTime) {
        if (isDismissed || (!repeats && fireCount[clause] > 0) || isRunning(clause)) {
            return false;
        }
        boolean waitsForPrevious = clause > 0 && spell.getClauses().get(clause).runsAfterPrevious();
        if (waitsForPrevious && fireCount[clause - 1] <= fireCount[clause]) {
            return false;
        }
        if (requirements[clause].length == 0) {
            if (!repeats || waitsForPrevious) {
                return true;
            }
            return gameTime - lastAttempt[clause] >= getAttemptIntervalTicks();
        }
        return areRequirementsMet(clause, gameTime);
    }

    private boolean areRequirementsMet(int clause, long gameTime) {
        int window = CabalistConfig.REQUIREMENT_WINDOW_TICKS.get();
        int groupCount = requirementGroups[clause].length == 0 ? 0 : requirementGroups[clause][requirementGroups[clause].length - 1] + 1;
        for (int group = 0; group < groupCount; group++) {
            boolean isGroupMet = true;
            for (int j = 0; j < requirements[clause].length; j++) {
                if (requirementGroups[clause][j] != group) {
                    continue;
                }
                long met = metAt[clause][j];
                if (met < 0 || gameTime - met > window) {
                    isGroupMet = false;
                    break;
                }
            }
            if (isGroupMet) {
                return true;
            }
        }
        return false;
    }

    private long getAttemptIntervalTicks() {
        return Math.max(1, Math.round(SpellParse.estimateCastSeconds(incantation) * 20));
    }

    private int fire(int clause, long gameTime) {
        lastAttempt[clause] = gameTime;
        Subject cause = lastCause[clause];
        for (int j = 0; j < metAt[clause].length; j++) {
            metAt[clause][j] = -1;
        }
        lastCause[clause] = null;

        Spell resolved = SpellEngine.INSTANCE.resolve(incantation, host, castSeconds);
        if (clause >= resolved.getClauses().size()) {
            return -1;
        }
        SpellClause toFire = resolved.getClauses().get(clause);
        if (toFire.getTarget() == null && cause != null) {
            toFire.setTarget(cause);
        }
        int chainEnd = getChainEnd(clause, resolved.getClauses().size());
        Spell single = new Spell(new ArrayList<>(resolved.getClauses().subList(clause, chainEnd)), host);
        single.setCastSeconds(castSeconds);
        single.setIncantation(incantation);
        single.setName(spell.getName());
        single.setSource(spell.getSource());
        single.setColors(spell.getColors());
        if (paymentStack != null) {
            single.shareEnergyStack(paymentStack);
        }
        if (repeats) {
            single.markInscribed();
        }
        // Whatever the host's death sets off has to outlive it.
        for (Term term : requirements[clause]) {
            if (term instanceof RequirementTerm requirement && term.getResourceLocation().equals(CabalistTerms.DIES.getId()) && requirement.isAboutHost(host)) {
                single.release();
            }
        }
        int affected = SpellExecutor.INSTANCE.castNow(single);
        if (affected < 0) {
            return -1;
        }
        for (int i = clause; i < chainEnd; i++) {
            fireCount[i]++;
            running[i] = single;
        }
        return affected;
    }

    private int getChainEnd(int clause, int resolvedCount) {
        int end = clause + 1;
        List<SpellClause> clauses = spell.getClauses();
        while (end < fireCount.length && end < resolvedCount && clauses.get(end).runsAfterPrevious() && requirements[end].length == 0
                && (repeats || fireCount[end] == 0)) {
            end++;
        }
        return end;
    }
}
