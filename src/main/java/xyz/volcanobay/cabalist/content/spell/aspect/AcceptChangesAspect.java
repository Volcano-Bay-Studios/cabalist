package xyz.volcanobay.cabalist.content.spell.aspect;

import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractDraft;
import xyz.volcanobay.cabalist.system.contract.DraftSystem;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.DraftCircleSubject;

import java.util.UUID;

// Submits the caster's draft as one amendment, aimed at the contract or any circle of the draft.
public class AcceptChangesAspect extends ContractAspect {

    @Override
    public boolean aimsWhereLooking() {
        return true;
    }

    @Override
    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        UUID caster = clause.getCaster().getUUID();
        for (ContractDraft draft : DraftSystem.INSTANCE.getDrafts()) {
            if (draft.getAuthor().equals(caster) && draft.isFor(contract)) {
                return DraftSystem.INSTANCE.submit(draft);
            }
        }
        return false;
    }

    @Override
    public boolean affectDraft(DraftCircleSubject circle, SpellClause clause, float magnitude) {
        return circle.getDraft().getAuthor().equals(clause.getCaster().getUUID()) && DraftSystem.INSTANCE.submit(circle.getDraft());
    }
}
