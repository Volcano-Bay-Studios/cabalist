package xyz.volcanobay.cabalist.system.form;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistAspects;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.networking.packet.SpellEnergyS2CPacket;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.render.FormShape;
import xyz.volcanobay.cabalist.system.render.SpellVisuals;
import xyz.volcanobay.cabalist.system.spell.EnergyLedger;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.spell.SpellExecutor;
import xyz.volcanobay.cabalist.system.spell.SpellPayment;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.subject.SubjectList;
import xyz.volcanobay.cabalist.system.visibility.HidingSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * a running clause. Starts with subjects, then proceeds with spell over multiple ticks
 */
public abstract class Delivery {
    protected final SpellClause clause;
    protected final Level level;
    protected final SubjectList batch = new SubjectList();
    private final IntArrayList visualIds = new IntArrayList();
    private final List<FormShape> shapes = new ArrayList<>();
    protected int age;
    private boolean isDelivered;
    private boolean isEnded;
    private double ambientPerTick = Double.NaN;

    protected Delivery(SpellClause clause, Level level) {
        this.clause = clause;
        this.level = level;
    }

    /**
     * Returns true once the delivery has reached everything it will first reach.
     */
    protected abstract boolean deliverTick();

    protected abstract void collectSustained(SubjectList out);

    protected void sustainTick() {
    }

    /**
     * Whether there is still anything for the delivery to work on. A delivery with nothing left ends early.
     */
    protected boolean hasSustainedSubjects() {
        return true;
    }

    protected abstract Subject getEndLocation();

    protected @Nullable Subject getEndHit() {
        return null;
    }

    public void start() {
        Spell spell = clause.getSpell();
        if (spell != null) {
            spell.onDeliveryStarted();
        }
        DeliverySystem.INSTANCE.add(this);
    }

    public boolean tick() {
        Spell spell = clause.getSpell();
        return tickDelivery() || (spell != null && spell.isFinishing());
    }

    private boolean tickDelivery() {
        if (shouldEnd()) {
            return true;
        }
        Spell ticking = clause.getSpell();
        if (ticking != null) {
            ticking.getLedger().tick(level.getGameTime());
            if (clause.getAspects().contains(CabalistAspects.HIDE.get()) && clause.getAspects().size() > 1 && !ticking.isInscribed()) {
                HidingSystem.INSTANCE.hideSpell(ticking, level.getGameTime());
            }
        }
        age++;
        if (!isDelivered) {
            if (deliverTick()) {
                isDelivered = true;
                clause.markSustaining();
                startContinuation();
                return shouldEnd() || !hasSustainedAspects();
            }
            return shouldEnd();
        }
        sustainTick();
        pulse();
        return shouldEnd() || !hasSustainedSubjects();
    }

    private boolean hasSustainedAspects() {
        for (Aspect aspect : clause.getAspects()) {
            if (aspect.isSustained()) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldEnd() {
        Spell spell = clause.getSpell();
        if (spell != null && !spell.isReleased() && spell.getHost() != null && !spell.getHost().isValid()) {
            spell.dismiss();
        }
        return isEnded || clause.isExhausted() || spell == null || spell.isDismissed();
    }

    private void pulse() {
        Spell spell = clause.getSpell();
        if (spell == null) {
            return;
        }
        if (paysFormUpkeep()) {
            double upkeep = CabalistConfig.HANGING_UPKEEP_PER_SECOND.get() / 20.0;
            spell.getEnergyStack().take(CabalistEnergyTypes.ENTROPY.get(), upkeep);
            spell.getLedger().spend(EnergyLedger.UPKEEP, upkeep);
            if (Double.isNaN(ambientPerTick)) {
                ambientPerTick = SpellPayment.INSTANCE.estimateClauseCost(clause) * CabalistConfig.FORM_AMBIENT_FRACTION_PER_SECOND.get() / 20.0;
            }
            spell.getEnergyStack().take(CabalistEnergyTypes.ENTROPY.get(), ambientPerTick);
            spell.getLedger().spend(EnergyLedger.AMBIENT, ambientPerTick);
            spell.noteSpent(upkeep + ambientPerTick);
            if (!SpellPayment.INSTANCE.settle(spell, clause)) {
                clause.markExhausted();
                return;
            }
        }
        batch.clear();
        collectSustained(batch);
        applyBatch();
    }

    private boolean paysFormUpkeep() {
        boolean isOnlyHiding = true;
        for (Aspect aspect : clause.getAspects()) {
            if (aspect == CabalistAspects.HIDE.get()) {
                continue;
            }
            isOnlyHiding = false;
            if (aspect.paysFormUpkeep()) {
                return true;
            }
        }
        return isOnlyHiding;
    }

    protected void applyBatch() {
        if (!batch.isEmpty()) {
            SpellExecutor.INSTANCE.apply(clause, batch);
            batch.clear();
        }
    }

    private void startContinuation() {
        List<SpellClause> continuation = clause.getContinuation();
        if (continuation == null || clause.isExhausted()) {
            return;
        }
        clause.setContinuation(null);
        SpellExecutor.INSTANCE.continueAt(continuation, getEndLocation(), getEndHit());
    }

    protected boolean isDelivered() {
        return isDelivered;
    }

    public void collectEnergy(List<SpellEnergyS2CPacket.Entry> out) {
        Spell spell = clause.getSpell();
        if (spell == null) {
            return;
        }
        float flow = (float) spell.getLedger().getTotalRate();
        float remaining = spell.getRemainingFraction();
        for (int i = 0; i < visualIds.size(); i++) {
            out.add(new SpellEnergyS2CPacket.Entry(visualIds.getInt(i), flow, remaining));
        }
    }

    public IntList getVisualIds() {
        return visualIds;
    }

    public boolean hasVisual(int visualId) {
        return visualIds.contains(visualId);
    }

    public @Nullable Spell getSpell() {
        return clause.getSpell();
    }

    public Level getLevel() {
        return level;
    }

    public void showVisual(FormShape shape) {
        visualIds.add(SpellVisuals.send(level, shape, clause));
        shapes.add(shape);
    }

    public List<FormShape> getShapes() {
        return shapes;
    }

    public SpellClause getClause() {
        return clause;
    }

    protected void updateVisual(int index, FormShape shape) {
        if (index < shapes.size()) {
            shapes.set(index, shape);
        }
        if (index < visualIds.size()) {
            SpellVisuals.update(level, visualIds.getInt(index), shape);
        }
    }

    public void end() {
        if (isEnded) {
            return;
        }
        isEnded = true;
        for (int i = 0; i < visualIds.size(); i++) {
            SpellVisuals.end(level, visualIds.getInt(i));
        }
        Spell spell = clause.getSpell();
        if (spell != null) {
            spell.onDeliveryEnded();
        }
    }
}
