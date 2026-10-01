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
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.Subject;


public class HealAspect extends Aspect {
    private ModConfigSpec.DoubleValue healthPerMagnitude;

    @Override
    protected void defineSettings(ModConfigSpec.Builder builder) {
        healthPerMagnitude = builder.defineInRange("health_per_magnitude", 1.0, 0, Double.MAX_VALUE);
    }

    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.LIFE.get();
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        double healed = EffectMeter.measure(magnitude * healthPerMagnitude.get());
        out.set(CabalistEnergyTypes.LIFEFORCE.get(), -healed * CabalistConfig.ENTROPY_PER_HEALTH.get());
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        if (!(entity instanceof LivingEntity living) || living.getHealth() >= living.getMaxHealth()) {
            return false;
        }
        float before = living.getHealth();
        living.heal((float) (magnitude * healthPerMagnitude.get()));
        EffectMeter.report(living.getHealth() - before);
        return living.getHealth() > before;
    }
}
