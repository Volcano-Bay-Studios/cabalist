package xyz.volcanobay.cabalist.system.spell;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.core.CabalistAspects;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.energy.SharedEnergyStack;
import xyz.volcanobay.cabalist.system.focus.SpellSource;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The host is the subject the spell lives on. ie its caster when spoken, or whatever it is inscribed on.
 */
public class Spell extends Subject {
    protected final List<SpellClause> clauses;
    protected EnergyStack stack = new EnergyStack();
    protected @Nullable Subject host;
    protected double castSeconds;
    protected String incantation = "";
    protected @Nullable String name;
    protected SpellSource source = SpellSource.NONE;
    protected double settledPressure;
    protected int runningDeliveries;
    protected boolean isDismissed;
    protected boolean isFinishing;
    protected double producedThisTick;
    protected final Map<UUID, Long> actedOn = new HashMap<>();
    protected double spentThisTick;
    protected final EnergyLedger ledger = new EnergyLedger();
    protected double initialAllotment;
    protected boolean isInscribed;
    protected boolean isReleased;
    protected long revealedToAllAt = Long.MIN_VALUE;
    protected List<Integer> colors = List.of();

    public Spell(List<SpellClause> clauses, @Nullable Subject host) {
        this.clauses = clauses;
        this.host = host;
        for (SpellClause clause : clauses) {
            clause.setSpell(this);
        }
    }

    public void shareEnergyStack(EnergyStack shared) {
        this.stack = new SharedEnergyStack(shared);
    }

    public boolean hasOwnEnergy() {
        return !isInscribed;
    }

    public boolean isReleased() {
        if (isReleased) {
            return true;
        }
        for (SpellClause clause : clauses) {
            if (clause.isReleased()) {
                return true;
            }
        }
        return false;
    }

    public void release() {
        isReleased = true;
    }

    public boolean hidesFromOwners() {
        for (SpellClause clause : clauses) {
            for (Aspect aspect : clause.getAspects()) {
                if (aspect.hidesFromOwners()) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Integer> getColors() {
        return colors;
    }

    public void setColors(List<Integer> colors) {
        this.colors = List.copyOf(colors);
    }

    public boolean isInscribed() {
        return isInscribed;
    }

    public void markInscribed() {
        isInscribed = true;
    }

    public boolean isRunning() {
        return runningDeliveries > 0;
    }

    public void onDeliveryStarted() {
        runningDeliveries++;
    }

    public void onDeliveryEnded() {
        runningDeliveries = Math.max(0, runningDeliveries - 1);
    }

    public EnergyLedger getLedger() {
        return ledger;
    }

    public void markInitialAllotment() {
        initialAllotment = getRemainingAllotment();
    }

    public void feed(double amount) {
        stack.give(CabalistEnergyTypes.ENTROPY.get(), amount);
        initialAllotment = Math.max(initialAllotment, getRemainingAllotment());
    }

    public float getRemainingFraction() {
        return initialAllotment <= 0 ? 0 : (float) Math.min(1, getRemainingAllotment() / initialAllotment);
    }

    public double getRemainingAllotment() {
        double debt = Math.max(0, stack.getWorldPressure() - settledPressure);
        return Math.max(0, stack.get(CabalistEnergyTypes.ENTROPY.get()) - debt);
    }

    public SpellSource getSource() {
        return source;
    }

    public void setSource(SpellSource source) {
        this.source = source;
    }

    public boolean isDismissed() {
        return isDismissed;
    }

    public void dismiss() {
        isDismissed = true;
    }

    /**
     * Acted on subjects, used to reveal hidden spells.
     */
    public Map<UUID, Long> getActedOn() {
        return actedOn;
    }

    public void revealToAll(long gameTime) {
        revealedToAllAt = gameTime;
    }

    public long getRevealedToAllAt() {
        return revealedToAllAt;
    }

    public void noteProduced(double amount) {
        producedThisTick += amount;
    }

    public void noteSpent(double amount) {
        spentThisTick += amount;
    }

    public void settleProduced() {
        double unused = Math.max(0, producedThisTick - spentThisTick);
        if (unused > 0) {
            double lost = stack.extract(CabalistEnergyTypes.ENTROPY.get(), unused);
            ledger.spend(EnergyLedger.WASTED, lost);
        }
        producedThisTick = 0;
        spentThisTick = 0;
    }

    public boolean isFinishing() {
        return isFinishing;
    }

    public void finish() {
        isFinishing = true;
    }

    @Override
    public boolean isValid() {
        return !isDismissed;
    }

    @Override
    public boolean receive(Aspect aspect, SpellClause clause, float magnitude) {
        return aspect.affectSpell(this, clause, magnitude);
    }

    @Override
    public EnergyStack getEnergyStack() {
        return stack;
    }

    public double getCastSeconds() {
        return castSeconds;
    }

    public void setCastSeconds(double castSeconds) {
        this.castSeconds = castSeconds;
    }

    public boolean isSpoken() {
        return castSeconds > 0;
    }

    /**
     * Capacity can increase with devotion.
     */
    public double getCapacityMultiplier() {
        double total = 0;
        for (SpellClause clause : clauses) {
            total += clause.getDevotion().getCapacityMultiplier(clause.getPrimaryDomain());
        }
        return clauses.isEmpty() ? 1 : total / clauses.size();
    }

    public double getSettledPressure() {
        return settledPressure;
    }

    public void setSettledPressure(double settledPressure) {
        this.settledPressure = settledPressure;
    }

    public String getIncantation() {
        return incantation;
    }

    public void setIncantation(String incantation) {
        this.incantation = incantation;
    }

    public boolean hasWaitingClauses() {
        for (SpellClause clause : clauses) {
            if (clause.hasRequirements()) {
                return true;
            }
        }
        return false;
    }

    public boolean isMaterializing() {
        Aspect materialize = CabalistAspects.MATERIALIZE.get();
        for (SpellClause clause : clauses) {
            if (clause.getAspects().contains(materialize)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAspectActive(Aspect aspect) {
        return aspect.isEnabled() && (aspect == CabalistAspects.MATERIALIZE.get() || !isMaterializing());
    }

    public @Nullable String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public List<SpellClause> getClauses() {
        return clauses;
    }

    public @Nullable Subject getHost() {
        return host;
    }

    public void setHost(@Nullable Subject host) {
        this.host = host;
    }

    @Override
    public @Nullable Level getLevel() {
        if (host == null) {
            return null;
        }
        return host.getLevel();
    }

    @Override
    public void getPosition(Vector3d out) {
        if (host == null) {
            out.zero();
            return;
        }
        host.getPosition(out);
    }

    @Override
    public void getFacing(Vector3d out) {
        if (host == null) {
            out.zero();
            return;
        }
        host.getFacing(out);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Spell");
    }
}
