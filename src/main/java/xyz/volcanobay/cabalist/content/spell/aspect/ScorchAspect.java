package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.ModConfigSpec;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.system.aspect.EffectMeter;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.Subject;

public class ScorchAspect extends BurningAspect {
    private ModConfigSpec.DoubleValue damagePerMagnitude;

    @Override
    protected void defineSettings(ModConfigSpec.Builder builder) {
        damagePerMagnitude = builder.defineInRange("damage_per_magnitude", 2.0, 0, Double.MAX_VALUE);
    }

    @Override
    protected double getCircleBurnEffect() {
        return 1;
    }

    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.ENTROPY.get();
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        double dealt = EffectMeter.measure(magnitude * damagePerMagnitude.get());
        out.set(CabalistEnergyTypes.ENTROPY.get(), -dealt * CabalistConfig.ENTROPY_PER_HEALTH.get());
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        float before = entity instanceof LivingEntity living ? living.getHealth() : 0;
        if (!entity.hurt(entity.damageSources().inFire(), (float) (magnitude * damagePerMagnitude.get()))) {
            return false;
        }
        if (entity instanceof LivingEntity living) {
            EffectMeter.report(Math.max(0, before - living.getHealth()));
        }
        return true;
    }
}
