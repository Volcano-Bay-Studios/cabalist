package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.ModConfigSpec;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EffectMeter;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

public class SacrificeAspect extends Aspect {
    private ModConfigSpec.DoubleValue damagePerMagnitude;

    @Override
    protected void defineSettings(ModConfigSpec.Builder builder) {
        damagePerMagnitude = builder.defineInRange("damage_per_magnitude", 8.0, 0, Double.MAX_VALUE);
    }

    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.LIFE.get();
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        out.set(CabalistEnergyTypes.ENTROPY.get(), EffectMeter.measure(magnitude * damagePerMagnitude.get()) * CabalistConfig.ENTROPY_PER_HEALTH.get());
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        Spell spell = clause.getSpell();
        if (!(entity instanceof LivingEntity living) || spell == null) {
            return false;
        }
        Subject target = clause.getTarget();
        boolean targetConsents = target != null && target.represents(entity) && target.consents(clause.getCaster(), spell);
        if (!targetConsents && !EntitySubject.of(entity).consents(clause.getCaster(), spell)) {
            return false;
        }
        float before = living.getHealth();
        if (!living.hurt(living.damageSources().magic(), (float) (magnitude * damagePerMagnitude.get()))) {
            return false;
        }
        EffectMeter.report(Math.max(0, before - living.getHealth()));
        return true;
    }
}
