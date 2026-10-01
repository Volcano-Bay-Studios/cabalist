package xyz.volcanobay.cabalist.content.spell.aspect;

import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.UUID;

// The arbiter, or the contract itself, hands the role to another member
public class AppointAspect extends ContractAspect {

    @Override
    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        UUID caster = clause.getCaster().getUUID();
        Subject appointed = clause.getContractMember();
        UUID appointedId = appointed == null ? null : appointed.getUUID();
        boolean isArbiter = caster != null && caster.equals(contract.getCreatorId()) || clause.getCaster().getBoundContract() == contract;
        if (!isArbiter || appointedId == null || !contract.hasMember(appointedId) || appointedId.equals(contract.getCreatorId())) {
            return false;
        }
        contract.setCreatorId(appointedId);
        contract.logAction(caster, "arbiter", appointedId.toString(), "accepted");
        return true;
    }
}
