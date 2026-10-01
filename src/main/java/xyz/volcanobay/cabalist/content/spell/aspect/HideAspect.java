package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistAspects;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.aspect.OncePerTick;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.visibility.HidingSystem;

// Keeps something hidden for as long as the spell pays for it. Spoken alongside other effects, it hides the spell itself instead.
public class HideAspect extends Aspect {
    private static final int EFFECT_TICKS = 10;
    private static final OncePerTick CHARGES = new OncePerTick();

    private ModConfigSpec.DoubleValue costPerTick;

    @Override
    protected void defineSettings(ModConfigSpec.Builder builder) {
        costPerTick = builder.defineInRange("cost_per_tick", 0.25, 0, Double.MAX_VALUE);
    }

    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.ENTROPY.get();
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        out.set(CabalistEnergyTypes.ENTROPY.get(), -costPerTick.get());
    }

    // Whatever it's hiding, nothing it reaches is shown the spell for it.
    @Override
    public boolean revealsToTarget() {
        return false;
    }

    @Override
    public boolean reachesWholeGroups() {
        return true;
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        if (hidesOwnSpell(clause)) {
            return hideOwnSpell(clause, entity.level().getGameTime());
        }
        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, EFFECT_TICKS, 0, false, false, false));
        }
        HidingSystem.INSTANCE.hideEntity(entity.getUUID(), entity.level().getGameTime());
        return true;
    }

    @Override
    public boolean affectSpell(Spell spell, SpellClause clause, float magnitude) {
        long gameTime = getGameTime(spell);
        if (hidesOwnSpell(clause)) {
            return hideOwnSpell(clause, gameTime);
        }
        HidingSystem.INSTANCE.hideSpell(spell, gameTime);
        return true;
    }

    @Override
    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        long gameTime = getGameTime(contract);
        if (hidesOwnSpell(clause)) {
            return hideOwnSpell(clause, gameTime);
        }
        HidingSystem.INSTANCE.hideContract(contract.getUUID(), gameTime);
        return true;
    }

    private static boolean hidesOwnSpell(SpellClause clause) {
        return clause.getAspects().size() > 1;
    }

    public static boolean hidesItself(Spell spell) {
        for (SpellClause clause : spell.getClauses()) {
            if (clause.getAspects().contains(CabalistAspects.HIDE.get()) && hidesOwnSpell(clause)) {
                return true;
            }
        }
        return false;
    }

    // Hiding its own spell is paid once a tick, however much else the spell reaches.
    private static boolean hideOwnSpell(SpellClause clause, long gameTime) {
        Spell spell = clause.getSpell();
        if (spell == null || spell.isInscribed()) {
            return false;
        }
        HidingSystem.INSTANCE.hideSpell(spell, gameTime);
        return CHARGES.tryPass(spell, gameTime);
    }

    private static long getGameTime(@Nullable Subject subject) {
        return subject == null || subject.getLevel() == null ? 0 : subject.getLevel().getGameTime();
    }
}
