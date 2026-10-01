package xyz.volcanobay.cabalist.content.spell.aspect;

import net.neoforged.neoforge.common.ModConfigSpec;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.Subject;

/**
 * Pulls entropy off another spell's stack onto this one, raising the other spell's cost.
 */
public class SiphonAspect extends Aspect {
    private ModConfigSpec.DoubleValue entropyPerMagnitude;

    @Override
    protected void defineSettings(ModConfigSpec.Builder builder) {
        entropyPerMagnitude = builder.defineInRange("entropy_per_magnitude", 5.0, 0, Double.MAX_VALUE);
    }

    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.ENTROPY.get();
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        out.clear();
    }

    @Override
    public boolean affectSpell(Spell spell, SpellClause clause, float magnitude) {
        Spell siphoner = clause.getSpell();
        if (siphoner == null || siphoner == spell) {
            return false;
        }
        Subject owner = spell.getHost();
        Subject caster = clause.getCaster();
        if (owner != null && owner != caster && !owner.consents(caster, siphoner)) {
            return false;
        }
        double moved = spell.getEnergyStack().extract(CabalistEnergyTypes.ENTROPY.get(), magnitude * entropyPerMagnitude.get());
        siphoner.getEnergyStack().give(CabalistEnergyTypes.ENTROPY.get(), moved);
        return moved > 0;
    }
}
