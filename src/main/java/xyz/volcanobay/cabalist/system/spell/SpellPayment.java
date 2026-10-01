package xyz.volcanobay.cabalist.system.spell;

import xyz.volcanobay.cabalist.content.spell.aspect.HarmAspect;
import xyz.volcanobay.cabalist.core.CabalistAspects;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EffectMeter;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.entropy.EntropyNetworkContract;
import xyz.volcanobay.cabalist.system.rift.RiftHelper;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.subject.SubjectList;


public class SpellPayment {
    public static final SpellPayment INSTANCE = new SpellPayment();

    private static final EnergyStack EMPTY = new EnergyStack();

    private final EnergyStack scratch = new EnergyStack();
    private final EnergyUse energyUse = new EnergyUse();
    private final SubjectList allBearers = new SubjectList();
    private final SubjectList bearers = new SubjectList();
    private final SubjectList members = new SubjectList();
    private final SubjectList harmed = new SubjectList();

    private double pressure;
    private double available;
    private double fromSpell;
    private double fromBearerStacks;
    private double fromLifeforce;
    private double unpaid;
    private double riftCost;

    public boolean pay(Spell spell) {
        riftCost = 0;
        pressure = 0;
        fromBearerStacks = 0;
        fromLifeforce = 0;
        unpaid = 0;
        scratch.copyFrom(spell.getEnergyStack());
        available = scratch.get(CabalistEnergyTypes.ENTROPY.get());

        spell.setSettledPressure(spell.getEnergyStack().getWorldPressure());
        if (!spell.isSpoken()) {
            for (SpellClause clause : spell.getClauses()) {
                pressure += estimatePressure(clause);
            }
            fromSpell = available - scratch.get(CabalistEnergyTypes.ENTROPY.get());
            unpaid = pressure;
            return pressure <= 0;
        }

        for (SpellClause clause : spell.getClauses()) {
            double coveredBefore = scratch.get(CabalistEnergyTypes.ENTROPY.get());
            double owed = estimatePressure(clause);
            double clauseCost = owed + coveredBefore - scratch.get(CabalistEnergyTypes.ENTROPY.get());
            pressure += owed;
            if (owed <= 0) {
                continue;
            }
            owed -= payFromBearers(spell, clause, owed);
            if (owed > 0) {
                unpaid += owed;
                clause.setPower(clause.getPower() * (float) Math.max(0, 1 - owed / clauseCost));
            }
        }
        fromSpell = available - scratch.get(CabalistEnergyTypes.ENTROPY.get());
        return true;
    }

    public boolean settle(Spell spell, SpellClause clause) {
        EnergyStack stack = spell.getEnergyStack();
        double owed = stack.getWorldPressure() - spell.getSettledPressure();
        if (owed <= 0) {
            return true;
        }
        if (spell.isSpoken() && !clause.isSustaining()) {
            double entropyBefore = stack.get(CabalistEnergyTypes.ENTROPY.get());
            owed -= payFromBearers(spell, clause, owed);
            stack.extract(CabalistEnergyTypes.ENTROPY.get(), stack.get(CabalistEnergyTypes.ENTROPY.get()) - entropyBefore);
        }
        spell.setSettledPressure(stack.getWorldPressure() - Math.max(owed, 0));
        if (owed > 0) {
            unpaid += owed;
            return false;
        }
        return true;
    }

    public double estimateCost(Spell spell) {
        double savedRiftCost = riftCost;
        scratch.copyFrom(EMPTY);
        double cost = 0;
        for (SpellClause clause : spell.getClauses()) {
            cost += estimatePressure(clause);
        }
        riftCost = savedRiftCost;
        return cost;
    }

    public double estimateClauseCost(SpellClause clause) {
        double savedRiftCost = riftCost;
        scratch.copyFrom(EMPTY);
        double cost = estimatePressure(clause);
        riftCost = savedRiftCost;
        return cost;
    }

    private double estimatePressure(SpellClause clause) {
        double before = scratch.getWorldPressure();
        Subject subject = clause.getTarget() != null ? clause.getTarget() : clause.getCaster();
        double formCost = clause.getForm() != null ? clause.getForm().getCostFactor(clause) : 1;
        double energyMoved = 0;
        for (Aspect aspect : clause.getAspects()) {
            if (clause.getSpell() != null && !clause.getSpell().isAspectActive(aspect)) {
                continue;
            }
            energyUse.clear();
            EffectMeter.begin();
            Devotion devotion = clause.getDevotion();
            float magnitude = (float) (clause.getPower() * aspect.getMagnitudeMultiplier() * devotion.getStrengthMultiplier(aspect.getDomain()));
            aspect.getEnergyUse(subject, clause, magnitude, energyUse);
            energyUse.scale(formCost * devotion.getCostMultiplier(aspect.getDomain()));
            Domain domain = clause.getDomain();
            if (domain != null) {
                domain.modifyEnergyUse(energyUse, clause);
            }
            energyMoved += Math.abs(energyUse.getAmount());
            energyUse.applyTo(scratch);
        }
        double clauseRiftCost = RiftHelper.getRiftCost(clause, energyMoved);
        riftCost += clauseRiftCost;
        scratch.take(CabalistEnergyTypes.ENTROPY.get(), clauseRiftCost);
        return scratch.getWorldPressure() - before;
    }

    private double payFromBearers(Spell spell, SpellClause clause, double owed) {
        bearers.clear();
        collectConsentingBearers(spell, clause, bearers);
        double stackPaid = payFromBearerStacks(spell, owed);
        fromBearerStacks += stackPaid;
        double lifeforcePaid = owed > stackPaid && clause.isLifeforceAllowed() ? payWithHarm(spell, clause, owed - stackPaid) : 0;
        fromLifeforce += lifeforcePaid;
        return stackPaid + lifeforcePaid;
    }

    private void collectConsentingBearers(Spell spell, SpellClause clause, SubjectList out) {
        members.clear();
        clause.getBearer().collectMembers(members);
        EntropyNetworkContract.collectTouching(clause.getCaster(), members);
        for (Subject member : members) {
            if (!out.contains(member) && member.consents(clause.getCaster(), spell)) {
                out.add(member);
            }
        }
    }

    private double payFromBearerStacks(Spell spell, double owed) {
        double total = 0;
        for (Subject bearer : bearers) {
            EnergyStack stack = bearer.getEnergyStack();
            if (stack != null) {
                total += stack.get(CabalistEnergyTypes.ENTROPY.get());
            }
        }
        if (total <= 0) {
            return 0;
        }
        double taking = Math.min(owed, total);
        double paid = 0;
        for (Subject bearer : bearers) {
            EnergyStack stack = bearer.getEnergyStack();
            if (stack == null) {
                continue;
            }
            double share = taking * stack.get(CabalistEnergyTypes.ENTROPY.get()) / total;
            double moved = stack.extract(CabalistEnergyTypes.ENTROPY.get(), share);
            spell.getEnergyStack().give(CabalistEnergyTypes.ENTROPY.get(), moved);
            paid += moved;
        }
        return paid;
    }

    private double payWithHarm(Spell spell, SpellClause clause, double owed) {
        double total = 0;
        for (Subject bearer : bearers) {
            total += bearer.getLifeforcePool();
        }
        if (total <= 0) {
            return 0;
        }
        double taking = Math.min(owed, total);
        HarmAspect harm = CabalistAspects.HARM.get();
        SpellClause balance = new SpellClause(clause.getCaster());
        balance.setSpell(spell);
        balance.markBalance();
        balance.addAspect(harm);
        double paid = 0;
        for (Subject bearer : bearers) {
            double entropy = taking * bearer.getLifeforcePool() / total;
            if (entropy <= 0) {
                continue;
            }
            float magnitude = (float) (entropy / CabalistConfig.ENTROPY_PER_HEALTH.get() / harm.getDamagePerMagnitude());
            balance.setPower(magnitude);
            EffectMeter.begin();
            if (!bearer.receive(harm, balance, magnitude)) {
                continue;
            }
            energyUse.clear();
            harm.getEnergyUse(bearer, balance, magnitude, energyUse);
            energyUse.applyTo(spell.getEnergyStack());
            paid += energyUse.getAmount();
        }
        return paid;
    }

    public double getPressure() {
        return pressure;
    }

    public double getAvailable() {
        return available;
    }

    public double getFromSpell() {
        return fromSpell;
    }

    public double getFromBearerStacks() {
        return fromBearerStacks;
    }

    public double getFromLifeforce() {
        return fromLifeforce;
    }

    public double getUnpaid() {
        return unpaid;
    }

    public double getRiftCost() {
        return riftCost;
    }
}
