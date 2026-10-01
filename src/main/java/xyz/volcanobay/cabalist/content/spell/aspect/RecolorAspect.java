package xyz.volcanobay.cabalist.content.spell.aspect;

import net.neoforged.neoforge.common.ModConfigSpec;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EffectMeter;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.render.Recoloring;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.Subject;

public class RecolorAspect extends Aspect {
    private ModConfigSpec.DoubleValue cost;

    @Override
    protected void defineSettings(ModConfigSpec.Builder builder) {
        cost = builder.defineInRange("cost", 0.5, 0, Double.MAX_VALUE);
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        out.set(CabalistEnergyTypes.ENTROPY.get(), -cost.get());
    }

    @Override
    public boolean aimsWhereLooking() {
        return true;
    }

    @Override
    public boolean isSustained() {
        return false;
    }

    @Override
    public boolean shakesOnReach() {
        return false;
    }

    @Override
    public boolean reachesWholeGroups() {
        return true;
    }

    @Override
    public boolean affectSpell(Spell spell, SpellClause clause, float magnitude) {
        if (clause.getColors().isEmpty() || spell == clause.getSpell()) {
            return false;
        }
        Recoloring.recolor(spell, clause.getColors());
        EffectMeter.report(cost.get());
        return true;
    }

    @Override
    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        if (clause.getColors().isEmpty()) {
            return false;
        }
        contract.recolorAll(clause.getColors());
        EffectMeter.report(cost.get());
        return true;
    }
}
