package xyz.volcanobay.cabalist.content.spell.aspect;

import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

import java.util.UUID;

public class LeaveContractAspect extends ContractAspect {

    @Override
    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        UUID caster = clause.getCaster().getUUID();
        if (caster == null || !contract.hasMember(caster)) {
            return false;
        }
        contract.removeMemberId(caster);
        contract.logAction(caster, "leave", "", "accepted");
        return true;
    }
}
