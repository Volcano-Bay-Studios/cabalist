package xyz.volcanobay.cabalist.content.spell.aspect;

import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.request.RequestKind;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

import java.util.UUID;

// Joins the caster, or whoever else is named, like the owner of the blood on a dagger.
public class JoinContractAspect extends ContractAspect {

    // Said without a contract, it acts on the one being looked at.
    @Override
    public boolean aimsWhereLooking() {
        return true;
    }

    @Override
    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        UUID caster = clause.getCaster().getUUID();
        if (caster == null) {
            return false;
        }
        Member member = resolveMember(clause);
        UUID joining = member == null || member.id().equals(contract.getUUID()) ? caster : member.id();
        if (contract.hasMember(joining)) {
            return false;
        }
        RequestSystem.INSTANCE.submitMembership(contract, RequestKind.JOIN, caster, joining, joining.equals(caster) || member != null && member.hasConsented());
        return true;
    }
}
