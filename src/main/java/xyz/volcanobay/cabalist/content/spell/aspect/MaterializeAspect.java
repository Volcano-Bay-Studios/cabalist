package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.block.RuneBlock;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;
import xyz.volcanobay.cabalist.core.CabalistDataComponents;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EffectMeter;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractState;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.DraftCircleSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.UUID;

/**
 * writes the rest of the spell down as a contract item, contract slate, rune.
 */
public class MaterializeAspect extends Aspect {
    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.SOVEREIGN.get();
    }

    @Override
    public boolean isSustained() {
        return false;
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        out.set(CabalistEnergyTypes.ENTROPY.get(), -EffectMeter.measure(0));
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        return false;
    }

    @Override
    public boolean affectItem(ItemStack stack, SpellClause clause, float magnitude) {
        String incantation = getIncantation(clause);
        if (incantation == null) {
            return false;
        }
        Contract existing = ContractSystem.INSTANCE.getContract(stack);
        if (existing != null) {
            return append(existing, clause, incantation);
        }
        Contract contract = createContract(clause, incantation);
        stack.set(CabalistDataComponents.CONTRACT.get(), contract.getUUID());
        return true;
    }

    @Override
    public boolean affectPosition(Level level, BlockPos pos, SpellClause clause, float magnitude) {
        String incantation = getIncantation(clause);
        if (incantation == null) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof ContractBlockEntity holder) {
            Contract existing = holder.getContract();
            if (existing != null) {
                return append(existing, clause, incantation);
            }
            holder.setContract(createContract(clause, incantation));
            return true;
        }
        BlockPos runePos = RuneBlock.getPlacePos(level, pos);
        Contract existing = ContractBlockEntity.getContractAt(level, runePos);
        if (existing != null) {
            return append(existing, clause, incantation);
        }
        ContractBlockEntity rune = RuneBlock.place(level, runePos);
        if (rune == null) {
            return false;
        }
        rune.setContract(createContract(clause, incantation));
        return true;
    }

    @Override
    public boolean affectDraft(DraftCircleSubject circle, SpellClause clause, float magnitude) {
        String incantation = getIncantation(clause);
        return incantation != null && circle.inscribe(incantation, clause.getCaster().getUUID());
    }

    private static @Nullable String getIncantation(SpellClause clause) {
        Spell spell = clause.getSpell();
        if (spell == null || spell.getIncantation().isEmpty()) {
            return null;
        }
        return spell.getIncantation();
    }

    private static boolean append(Contract contract, SpellClause clause, String incantation) {
        UUID caster = clause.getCaster().getUUID();
        if (caster == null) {
            return false;
        }
        return RequestSystem.INSTANCE.submitAmend(contract, caster, ContractState.of(contract).withLine(new ContractState.Line(incantation, ContractState.NEW_LINE)));
    }

    private static Contract createContract(SpellClause clause, String incantation) {
        UUID caster = clause.getCaster().getUUID();
        Contract contract = ContractSystem.INSTANCE.create("", caster);
        contract.addSpell(incantation, caster);
        imbue(contract, clause);
        return contract;
    }

    // The contract is imbued with whatever energy the inscribing spell has left, and starts full.
    private static void imbue(Contract contract, SpellClause clause) {
        Spell spell = clause.getSpell();
        double energy = spell == null ? 0 : spell.getRemainingAllotment();
        contract.imbue(energy);
        contract.getEnergyStack().give(CabalistEnergyTypes.ENTROPY.get(), energy);
        EffectMeter.report(energy);
    }
}
