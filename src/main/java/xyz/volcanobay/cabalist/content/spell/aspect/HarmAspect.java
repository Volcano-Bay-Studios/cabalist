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
import xyz.volcanobay.cabalist.system.energy.Lifeforce;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.Subject;

/**
 * Never takes a living thing below one heart. Only {@link SacrificeAspect} can kill.
 */
public class HarmAspect extends Aspect {
    private ModConfigSpec.DoubleValue damagePerMagnitude;
    private ModConfigSpec.DoubleValue minimumHealth;
    private ModConfigSpec.DoubleValue recoveredFraction;

    @Override
    protected void defineSettings(ModConfigSpec.Builder builder) {
        damagePerMagnitude = builder.defineInRange("damage_per_magnitude", 4.0, 0.01, Double.MAX_VALUE);
        minimumHealth = builder.defineInRange("minimum_health", 2.0, 0, Double.MAX_VALUE);
        // Hurting something costs what healing it would, and gives back only part of that as entropy for the same spell.
        recoveredFraction = builder.defineInRange("recovered_fraction", 0.5, 0, 1);
    }

    public float getDamagePerMagnitude() {
        return damagePerMagnitude.get().floatValue();
    }

    public float getMinimumHealth() {
        return minimumHealth.get().floatValue();
    }

    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.LIFE.get();
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        double dealt = EffectMeter.measure(magnitude * getDamagePerMagnitude());
        double released = dealt * CabalistConfig.ENTROPY_PER_HEALTH.get();
        // Balance is a spell drawing on its own bearer's life to pay for itself, which converts at full value.
        if (clause.isBalance()) {
            out.set(CabalistEnergyTypes.ENTROPY.get(), released);
            return;
        }
        out.set(CabalistEnergyTypes.ENTROPY.get(), -released);
        out.setProduced(released * recoveredFraction.get());
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        float damage = Math.min(magnitude * getDamagePerMagnitude(), living.getHealth() - getMinimumHealth());
        if (damage <= 0) {
            return false;
        }
        float before = living.getHealth();
        if (clause.isBalance()) {
            double lifeforce = Lifeforce.get(living);
            living.setHealth(living.getHealth() - damage);
            Lifeforce.set(living, lifeforce - damage);
        } else if (!living.hurt(living.damageSources().magic(), damage)) {
            return false;
        }
        EffectMeter.report(Math.max(0, before - living.getHealth()));
        return true;
    }
}
