package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EffectMeter;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.focus.LifeforceFlow;
import xyz.volcanobay.cabalist.system.request.PartyStatus;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.EnergyLedger;
import xyz.volcanobay.cabalist.system.spell.HangingSpellSystem;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.DraftCircleSubject;
import xyz.volcanobay.cabalist.system.subject.ItemSubject;
import xyz.volcanobay.cabalist.system.subject.NoticeCircleSubject;
import xyz.volcanobay.cabalist.system.subject.RequestCircleSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

public class DispelAspect extends Aspect {
    private ModConfigSpec.DoubleValue entropyCost;
    private static final double EMPTY = 1e-3;
    private static final double RELEASE_HEIGHT = 1;

    @Override
    protected void defineSettings(ModConfigSpec.Builder builder) {
        entropyCost = builder.defineInRange("entropy_cost", 2.0, 0, Double.MAX_VALUE);
    }

    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.ENTROPY.get();
    }

    @Override
    public boolean isSustained() {
        return false;
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        out.set(CabalistEnergyTypes.ENTROPY.get(), -EffectMeter.measure(magnitude * entropyCost.get()));
    }

    @Override
    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        if (!RequestSystem.INSTANCE.destroyOrPropose(contract, clause.getCaster())) {
            return false;
        }
        EffectMeter.report(0);
        return true;
    }

    @Override
    public boolean affectPosition(Level level, BlockPos pos, SpellClause clause, float magnitude) {
        Contract contract = ContractBlockEntity.getContractAt(level, pos);
        return contract != null && affectContract(contract, clause, magnitude);
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        if (!(entity instanceof ItemEntity item)) {
            return false;
        }
        ItemStack stack = item.getItem().copy();
        if (entity.level() instanceof ServerLevel level && LifeforceFlow.release(stack, level, entity.position().add(0, RELEASE_HEIGHT, 0))) {
            item.setItem(stack);
            EffectMeter.report(0);
            return true;
        }
        return affectItem(item.getItem(), clause, magnitude);
    }

    @Override
    public boolean affectItem(ItemSubject item, SpellClause clause, float magnitude) {
        Vector3d at = new Vector3d();
        item.getPosition(at);
        if (item.getLevel() instanceof ServerLevel level && LifeforceFlow.release(item.getStack(), level, new Vec3(at.x, at.y + RELEASE_HEIGHT, at.z))) {
            EffectMeter.report(0);
            return true;
        }
        return affectItem(item.getStack(), clause, magnitude);
    }

    @Override
    public boolean affectItem(ItemStack stack, SpellClause clause, float magnitude) {
        Contract contract = ContractSystem.INSTANCE.getContract(stack);
        return contract != null && affectContract(contract, clause, magnitude);
    }

    @Override
    public boolean affectDraft(DraftCircleSubject circle, SpellClause clause, float magnitude) {
        return circle.remove(true, clause.getCaster());
    }

    @Override
    public boolean affectNotice(NoticeCircleSubject circle, SpellClause clause, float magnitude) {
        return circle.dismiss(clause.getCaster().getUUID());
    }

    @Override
    public boolean affectRequest(RequestCircleSubject circle, SpellClause clause, float magnitude) {
        return circle.answer(PartyStatus.DISPELLED, clause.getCaster().getUUID());
    }

    @Override
    public boolean affectSpell(Spell spell, SpellClause clause, float magnitude) {
        if (spell == clause.getSpell()) {
            spell.finish();
            return true;
        }
        // An inscribed spell's energy belongs to its contract or network, so it just ends and fires again later.
        if (!spell.hasOwnEnergy()) {
            spell.dismiss();
            return true;
        }
        // Takes out only as much as this spell has, and the other spell shatters once it's empty.
        Spell own = clause.getSpell();
        EnergyStack stack = spell.getEnergyStack();
        double held = stack.get(CabalistEnergyTypes.ENTROPY.get());
        double removed = Math.min(held, own == null ? 0 : own.getRemainingAllotment());
        if (removed <= 0 && held > EMPTY) {
            return false;
        }
        stack.extract(CabalistEnergyTypes.ENTROPY.get(), removed);
        spell.getLedger().spend(EnergyLedger.DISMISSED, removed);
        EffectMeter.report(removed);
        if (held - removed <= EMPTY) {
            HangingSpellSystem.INSTANCE.removeSpell(spell);
            spell.dismiss();
        }
        return true;
    }
}
