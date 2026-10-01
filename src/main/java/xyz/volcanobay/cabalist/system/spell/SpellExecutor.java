package xyz.volcanobay.cabalist.system.spell;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.core.CabalistAspects;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.core.CabalistForms;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EffectMeter;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.form.Form;
import xyz.volcanobay.cabalist.system.form.InstantForm;
import xyz.volcanobay.cabalist.system.form.PointDelivery;
import xyz.volcanobay.cabalist.system.render.SpellVisuals;
import xyz.volcanobay.cabalist.system.rift.RiftHelper;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.subject.SubjectList;

import java.util.List;

/**
 * Server thread only. Reuses its lists, so applying a clause must not apply another clause.
 */
public class SpellExecutor {
    public static final SpellExecutor INSTANCE = new SpellExecutor();

    private final SubjectList reached = new SubjectList();
    private final SubjectList members = new SubjectList();

    private final EnergyUse energyUse = new EnergyUse();
    private final Vector3d shakePosition = new Vector3d();
    private double lastEnergyMoved;
    private double shakeEnergy;

    public int cast(Spell spell) {
        Subject host = spell.getHost();
        if (spell.hasWaitingClauses() && host != null && !spell.isMaterializing()) {
            PendingSpell pending = new PendingSpell(spell, host, false, spell.getEnergyStack());
            int affected = host.getLevel() == null ? 0 : pending.fireReady(host.getLevel().getGameTime());
            if (!pending.isComplete()) {
                HangingSpellSystem.INSTANCE.add(pending);
            }
            return affected;
        }
        int affected = castNow(spell);
        if (host != null && spell.isRunning()) {
            HangingSpellSystem.INSTANCE.add(PendingSpell.running(spell, host));
        }
        return affected;
    }

    public int castNow(Spell spell) {
        if (!SpellPayment.INSTANCE.pay(spell)) {
            return -1;
        }
        spell.markInitialAllotment();
        int affected = castClauses(spell.getClauses());
        spell.settleProduced();
        return affected;
    }

    public int castClauses(List<SpellClause> clauses) {
        int affected = 0;
        for (int i = 0; i < clauses.size(); i++) {
            SpellClause clause = clauses.get(i);
            boolean nextWaits = i + 1 < clauses.size() && clauses.get(i + 1).runsAfterPrevious();
            if (nextWaits && clause.getForm() != null && clause.getForm().completesLater()) {
                clause.setContinuation(clauses.subList(i + 1, clauses.size()));
                return affected + castClause(clause);
            }
            affected += castClause(clause);
        }
        return affected;
    }

    /**
     * Casts the rest of a spell from where a timed form ended. Clauses that only inherited their target
     * take what was hit instead, or the end point whne it misses
     */
    public int continueAt(List<SpellClause> continuation, Subject location, @Nullable Subject hit) {
        for (SpellClause clause : continuation) {
            clause.setLocation(location);
            if (clause.isTargetInherited()) {
                clause.setTarget(hit != null ? hit : location);
            }
        }
        return castClauses(continuation);
    }

    private int castClause(SpellClause clause) {
        Form form = clause.getForm();
        Subject target = clause.getTarget();
        if (form != null) {
            return form.deliver(clause, this);
        }
        if (target != null) {
            reached.clear();
            reached.add(target);
            return deliverToPoints(clause, reached, true);
        }
        return CabalistForms.WANDER.get().deliver(clause, this);
    }

    public int deliverInstantly(SpellClause clause, InstantForm form) {
        reached.clear();
        form.collectSubjects(clause, reached);
        return deliverToPoints(clause, reached, false);
    }

    private int deliverToPoints(SpellClause clause, SubjectList subjects, boolean chargesRift) {
        Level level = clause.getLocation().getLevel();
        if (level == null) {
            int affected = apply(clause, subjects);
            if (chargesRift) {
                chargeRift(clause);
            }
            return affected;
        }
        PointDelivery delivery = new PointDelivery(clause, level, subjects, chargesRift);
        delivery.start();
        return delivery.deliverNow();
    }

    private static String getLedgerName(Aspect aspect) {
        ResourceLocation id = CabalistAspects.ASPECT_REGISTRY.getKey(aspect);
        return id == null ? "aspect" : id.getPath();
    }

    public void chargeRift(SpellClause clause) {
        Spell spell = clause.getSpell();
        if (spell == null) {
            return;
        }
        double riftCost = RiftHelper.getRiftCost(clause, lastEnergyMoved);
        spell.getEnergyStack().take(CabalistEnergyTypes.ENTROPY.get(), riftCost);
        spell.getLedger().spend(EnergyLedger.RIFT, riftCost);
        spell.noteSpent(riftCost);
        if (!SpellPayment.INSTANCE.settle(spell, clause)) {
            clause.markExhausted();
        }
    }

    public int apply(SpellClause clause, SubjectList subjects) {
        int affected = applyAspects(clause, subjects);
        if (!clause.isBalance() && clause.getForm() != null && !subjects.isEmpty()) {
            Subject first = subjects.get(0);
            Level level = first.getLevel();
            if (level != null) {
                first.getPosition(shakePosition);
                SpellVisuals.shake(level, shakePosition, shakeEnergy);
            }
        }
        return affected;
    }

    private int applyAspects(SpellClause clause, SubjectList subjects) {
        Spell spell = clause.getSpell();
        lastEnergyMoved = 0;
        shakeEnergy = 0;
        if (spell == null || clause.isExhausted()) {
            return 0;
        }
        members.clear();
        for (Subject subject : subjects) {
            subject.collectMembers(members);
        }

        EnergyStack stack = spell.getEnergyStack();
        float magnitude = clause.getPower();
        int affected = 0;
        for (Aspect aspect : clause.getAspects()) {
            if (!clause.isBalance() && !spell.isAspectActive(aspect)) {
                continue;
            }
            if (clause.isSustaining() && !aspect.isSustained()) {
                continue;
            }
            SubjectList receivers = aspect.reachesWholeGroups() ? subjects : members;
            Domain aspectDomain = aspect.getDomain();
            Devotion devotion = clause.getDevotion();
            float aspectMagnitude = (float) (magnitude * aspect.getMagnitudeMultiplier() * devotion.getStrengthMultiplier(aspectDomain));
            double costMultiplier = devotion.getCostMultiplier(aspectDomain);
            for (Subject member : receivers) {
                EffectMeter.begin();
                if (!member.receive(aspect, clause, aspectMagnitude)) {
                    continue;
                }
                Level memberLevel = member.getLevel();
                if (aspect.revealsToTarget() && member.getUUID() != null && memberLevel != null) {
                    spell.getActedOn().put(member.getUUID(), memberLevel.getGameTime());
                }
                affected++;
                energyUse.clear();
                aspect.getEnergyUse(member, clause, aspectMagnitude, energyUse);
                energyUse.scale(costMultiplier);
                Domain domain = clause.getDomain();
                if (domain != null) {
                    domain.modifyEnergyUse(energyUse, clause);
                }
                lastEnergyMoved += Math.abs(energyUse.getAmount());
                if (aspect.shakesOnReach()) {
                    shakeEnergy += Math.abs(energyUse.getAmount());
                }
                if (!clause.isBalance()) {
                    spell.getLedger().spend(getLedgerName(aspect), -energyUse.getAmount());
                }
                energyUse.applyTo(stack);
                if (energyUse.getProduced() > 0) {
                    spell.noteProduced(energyUse.getProduced());
                } else if (energyUse.getAmount() < 0) {
                    spell.noteSpent(-energyUse.getAmount());
                }
                if (!clause.isBalance() && !SpellPayment.INSTANCE.settle(spell, clause)) {
                    clause.markExhausted();
                    return affected;
                }
            }
        }
        return affected;
    }
}
