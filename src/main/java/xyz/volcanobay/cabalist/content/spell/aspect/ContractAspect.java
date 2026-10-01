package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.blood.BloodStain;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.BloodSubject;
import xyz.volcanobay.cabalist.system.subject.ItemSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.UUID;

public abstract class ContractAspect extends Aspect {
    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.SOVEREIGN.get();
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        out.clear();
    }

    // Aimed at a block or item that holds a contract, it acts on that contract.
    @Override
    public boolean affectPosition(Level level, BlockPos pos, SpellClause clause, float magnitude) {
        Contract contract = ContractBlockEntity.getContractAt(level, pos);
        return contract != null && affectContract(contract, clause, magnitude);
    }

    @Override
    public boolean affectItem(ItemStack stack, SpellClause clause, float magnitude) {
        Contract contract = ContractSystem.INSTANCE.getContract(stack);
        return contract != null && affectContract(contract, clause, magnitude);
    }

    public record Member(UUID id, boolean hasConsented) {
    }

    // Who a membership spell is about. An item stands for whoever's blood is on it, or for the contract inscribed on it,
    // and blood given with a sacrificial dagger is that person's consent.
    protected static @Nullable Member resolveMember(SpellClause clause) {
        Subject member = clause.getContractMember();
        if (member instanceof ItemSubject item) {
            BloodStain blood = item.getBlood();
            if (blood != null) {
                return new Member(blood.owner(), blood.consented());
            }
            Contract contract = item.getBoundContract();
            return contract == null ? null : new Member(contract.getUUID(), false);
        }
        if (member instanceof BloodSubject blood && blood.getUUID() != null) {
            return new Member(blood.getUUID(), clause.getSpell() != null && blood.consents(clause.getCaster(), clause.getSpell()));
        }
        return member == null || member.getUUID() == null ? null : new Member(member.getUUID(), false);
    }

    @Override
    public boolean reachesWholeGroups() {
        return true;
    }

    @Override
    public boolean isSustained() {
        return false;
    }
}
