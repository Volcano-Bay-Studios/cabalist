package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
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


public class FreezeAspect extends Aspect {
    private ModConfigSpec.DoubleValue entropyGained;
    private ModConfigSpec.IntValue frozenTicksPerMagnitude;

    @Override
    protected void defineSettings(ModConfigSpec.Builder builder) {
        entropyGained = builder.defineInRange("entropy_gained", 2.0, 0, Double.MAX_VALUE);
        frozenTicksPerMagnitude = builder.defineInRange("frozen_ticks_per_magnitude", 200, 1, Integer.MAX_VALUE);
    }

    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.ENTROPY.get();
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        // Pulling the heat out costs what it moves, and what it moves is only worth something if the spell uses it.
        double moved = EffectMeter.measure(magnitude) * entropyGained.get();
        out.set(CabalistEnergyTypes.ENTROPY.get(), -moved);
        out.setProduced(moved);
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        if (!entity.canFreeze()) {
            return false;
        }
        // Freezes up to how cold the magnitude allows; only the added cold counts, so holding something frozen gains nothing.
        int before = entity.getTicksFrozen();
        int target = (int) (magnitude * frozenTicksPerMagnitude.get());
        if (target <= before) {
            return false;
        }
        int amountToApply = (target - before) / 10 + before;
        entity.clearFire();
        entity.setTicksFrozen(amountToApply);
        EffectMeter.report((target - before) / (double) frozenTicksPerMagnitude.get());
        return true;
    }

    @Override
    public boolean affectPosition(Level level, BlockPos pos, SpellClause clause, float magnitude) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BaseFireBlock) {
            level.removeBlock(pos, false);
            return true;
        }
        if (state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource()) {
            level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
            return true;
        }
        if (state.getFluidState().is(Fluids.LAVA) && state.getFluidState().isSource()) {
            level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
            return true;
        }
        return false;
    }
}
