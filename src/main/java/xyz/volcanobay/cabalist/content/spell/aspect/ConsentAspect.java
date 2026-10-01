package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.server.level.ServerPlayer;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.WorldContractee;
import xyz.volcanobay.cabalist.system.request.PartyStatus;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.request.WorldConsent;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.RequestCircleSubject;

import java.util.UUID;

// Consent needs a target. Aimed at a contract it answers that contract's requests, and an admin in world mode also answers for the world.
public class ConsentAspect extends ContractAspect {
    private final boolean consents;

    public ConsentAspect(boolean consents) {
        this.consents = consents;
    }

    @Override
    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        UUID caster = clause.getCaster().getUUID();
        if (caster == null) {
            return false;
        }
        PartyStatus status = consents ? PartyStatus.YES : PartyStatus.NO;
        int answered = RequestSystem.INSTANCE.answerContract(contract, caster, status, null);
        if (clause.getCaster() instanceof EntitySubject subject && subject.getEntity() instanceof ServerPlayer player && WorldConsent.isActive(player)) {
            answered += RequestSystem.INSTANCE.answerContract(contract, WorldContractee.WORLD_ID, status, caster);
        }
        return answered > 0;
    }

    @Override
    public boolean aimsWhereLooking() {
        return true;
    }

    @Override
    public boolean affectRequest(RequestCircleSubject circle, SpellClause clause, float magnitude) {
        return circle.answer(consents ? PartyStatus.YES : PartyStatus.NO, clause.getCaster().getUUID());
    }
}
