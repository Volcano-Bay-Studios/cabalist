package xyz.volcanobay.cabalist.content.spell.aspect;

import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.request.RequestKind;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

import java.util.UUID;

/**
 * Invites someone into a contract or expels them from it. Either needs their consent.
 */
public class MembershipAspect extends ContractAspect {
    private final RequestKind kind;

    public MembershipAspect(RequestKind kind) {
        this.kind = kind;
    }

    // Said without a contract, it acts on the one being looked at.
    @Override
    public boolean aimsWhereLooking() {
        return true;
    }

    @Override
    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        UUID caster = clause.getCaster().getUUID();
        Member member = resolveMember(clause);
        UUID memberId = member == null ? null : member.id();
        if (caster == null || memberId == null) {
            return false;
        }
        boolean isMember = contract.hasMember(memberId);
        if ((kind == RequestKind.JOIN) == isMember) {
            return false;
        }
        // Only an arbiter can bring someone else in: the creator, the contract itself, or a parent contract's arbiter.
        boolean isArbiter = contract.isArbiter(clause.getCaster());
        if (kind == RequestKind.JOIN && !memberId.equals(caster) && !isArbiter) {
            return false;
        }
        RequestSystem.INSTANCE.submitMembership(contract, kind, caster, memberId, member.hasConsented());
        return true;
    }
}
