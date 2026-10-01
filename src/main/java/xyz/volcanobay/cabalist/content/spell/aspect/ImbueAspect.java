package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EffectMeter;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.focus.Focus;
import xyz.volcanobay.cabalist.system.focus.Imbuement;
import xyz.volcanobay.cabalist.system.focus.SpellSource;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.Subject;

public class ImbueAspect extends Aspect {
    private ModConfigSpec.DoubleValue imbuePerMagnitude;
    private ModConfigSpec.DoubleValue copyEfficiency;

    @Override
    protected void defineSettings(ModConfigSpec.Builder builder) {
        imbuePerMagnitude = builder.defineInRange("imbue_per_magnitude", 2.0, 0, Double.MAX_VALUE);
        copyEfficiency = builder.defineInRange("copy_efficiency", 0.1, 0, 1);
    }

    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.LIFE.get();
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        out.set(CabalistEnergyTypes.ENTROPY.get(), -EffectMeter.measure(magnitude * imbuePerMagnitude.get()));
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        return entity instanceof LivingEntity living && affectItem(living.getMainHandItem(), clause, magnitude);
    }

    @Override
    public boolean affectItem(ItemStack stack, SpellClause clause, float magnitude) {
        Spell spell = clause.getSpell();
        if (spell == null || !Focus.canHold(stack) || Focus.getStored(stack).receptive()) {
            return false;
        }
        double energy = Math.min(magnitude * imbuePerMagnitude.get(), spell.getRemainingAllotment());
        if (energy <= 0) {
            return false;
        }
        Focus.update(stack, Imbuement::asReceptive);
        EffectMeter.report(energy);
        return true;
    }

    @Override
    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        Spell spell = clause.getSpell();
        if (spell == null) {
            return false;
        }
        SpellSource source = spell.getSource();
        double energy = Math.min(magnitude * imbuePerMagnitude.get(), spell.getRemainingAllotment());
        if (energy <= 0 || source.getTotal() <= 0) {
            return false;
        }
        contract.imbue(energy * copyEfficiency.get());
        EffectMeter.report(energy);
        return true;
    }
}
