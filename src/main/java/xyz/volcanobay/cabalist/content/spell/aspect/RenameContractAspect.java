package xyz.volcanobay.cabalist.content.spell.aspect;

import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractState;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.DraftCircleSubject;

import java.util.UUID;

public class RenameContractAspect extends ContractAspect {

    @Override
    public boolean aimsWhereLooking() {
        return true;
    }

    @Override
    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        UUID caster = clause.getCaster().getUUID();
        String name = clause.getContractName();
        if (caster == null || name == null || name.isEmpty()) {
            return false;
        }
        return RequestSystem.INSTANCE.submitAmend(contract, caster, ContractState.of(contract).withName(name));
    }

    @Override
    public boolean affectDraft(DraftCircleSubject circle, SpellClause clause, float magnitude) {
        String name = clause.getContractName();
        return name != null && !name.isEmpty() && circle.rename(name, clause.getCaster().getUUID());
    }
}
