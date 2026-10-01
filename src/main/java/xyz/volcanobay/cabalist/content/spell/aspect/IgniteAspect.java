package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.system.aspect.EffectMeter;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.util.BlockHelper;

public class IgniteAspect extends BurningAspect {
    private ModConfigSpec.DoubleValue burnSecondsPerMagnitude;
    private ModConfigSpec.DoubleValue burnDamagePerSecond;
    private ModConfigSpec.DoubleValue fireBlockSeconds;
    @Override
    protected void defineSettings(ModConfigSpec.Builder builder) {
        burnSecondsPerMagnitude = builder.defineInRange("burn_seconds_per_magnitude", 4.0, 0, Double.MAX_VALUE);
        burnDamagePerSecond = builder.defineInRange("burn_damage_per_second", 1.0, 0, Double.MAX_VALUE);
        // its like how long its burning in regards to the cost
        // * amount lit for cost
        fireBlockSeconds = builder.defineInRange("fire_block_seconds", 1.0, 0, Double.MAX_VALUE);
    }

    @Override
    protected double getCircleBurnEffect() {
        return fireBlockSeconds.get();
    }

    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.ENTROPY.get();
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        double burnSeconds = EffectMeter.measure(magnitude * burnSecondsPerMagnitude.get());
        out.set(CabalistEnergyTypes.ENTROPY.get(), -burnSeconds * burnDamagePerSecond.get() * CabalistConfig.ENTROPY_PER_HEALTH.get());
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        int before = entity.getRemainingFireTicks();
        entity.igniteForSeconds((float) (magnitude * burnSecondsPerMagnitude.get()));
        int added = entity.getRemainingFireTicks() - before;
        if (added <= 0) {
            return false;
        }
        EffectMeter.report(added / 20.0);
        return true;
    }

    @Override
    public boolean affectPosition(Level level, BlockPos pos, SpellClause clause, float magnitude) {
        int amountLit = BlockHelper.placeFireOn(level, pos); // light per face
        if (amountLit <= 0) {
            return false;
        }
        EffectMeter.report(fireBlockSeconds.get() * amountLit);
        return true;
    }
}
