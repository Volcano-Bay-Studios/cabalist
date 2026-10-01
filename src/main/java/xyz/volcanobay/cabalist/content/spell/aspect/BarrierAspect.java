package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import xyz.volcanobay.cabalist.core.CabalistAspects;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.aspect.OncePerTick;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.EnergyLedger;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.Subject;

public class BarrierAspect extends Aspect {
    private static final OncePerTick CHARGES = new OncePerTick();

    private ModConfigSpec.DoubleValue costPerSecond;
    private ModConfigSpec.DoubleValue costPerBlock;

    @Override
    protected void defineSettings(ModConfigSpec.Builder builder) {
        costPerSecond = builder.defineInRange("cost_per_second", 2.0, 0, Double.MAX_VALUE);
        costPerBlock = builder.defineInRange("cost_per_block", 0.25, 0, Double.MAX_VALUE);
    }

    public static boolean isBarrier(SpellClause clause) {
        Aspect barrier = CabalistAspects.BARRIER.get();
        Spell spell = clause.getSpell();
        return clause.getAspects().contains(barrier) && spell != null && !spell.isDismissed() && (clause.isBalance() || spell.isAspectActive(barrier));
    }

    public void chargeBlock(Spell spell) {
        double cost = costPerBlock.get();
        spell.getEnergyStack().take(CabalistEnergyTypes.ENTROPY.get(), cost);
        spell.getLedger().spend(EnergyLedger.BLOCKED, cost);
        spell.noteSpent(cost);
    }

    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.ENTROPY.get();
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        out.set(CabalistEnergyTypes.ENTROPY.get(), -costPerSecond.get() / 20);
    }

    @Override
    public boolean shakesOnReach() {
        return false;
    }

    @Override
    public boolean revealsToTarget() {
        return false;
    }

    @Override
    public boolean affectPosition(Level level, BlockPos pos, SpellClause clause, float magnitude) {
        return chargeOnce(clause, level.getGameTime());
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        return chargeOnce(clause, entity.level().getGameTime());
    }

    private static boolean chargeOnce(SpellClause clause, long gameTime) {
        Spell spell = clause.getSpell();
        return spell != null && CHARGES.tryPass(spell, gameTime);
    }
}
