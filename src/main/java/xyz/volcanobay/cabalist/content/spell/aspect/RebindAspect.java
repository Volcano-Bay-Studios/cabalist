package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractState;
import xyz.volcanobay.cabalist.system.contract.DraftSystem;
import xyz.volcanobay.cabalist.system.contract.HostTarget;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.PositionSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.UUID;

// Aimed at where the contract should go. It goes into the caster's open draft, or else is proposed straight away for the contract they hold.
public class RebindAspect extends ContractAspect {

    @Override
    public boolean aimsWhereLooking() {
        return true;
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        return rebind(EntitySubject.of(entity), clause);
    }

    @Override
    public boolean affectPosition(Level level, BlockPos pos, SpellClause clause, float magnitude) {
        return rebind(PositionSubject.ofBlock(level, pos), clause);
    }

    private static boolean rebind(Subject destination, SpellClause clause) {
        UUID caster = clause.getCaster().getUUID();
        HostTarget target = HostTarget.of(destination);
        if (caster == null || target == null) {
            return false;
        }
        if (DraftSystem.INSTANCE.setRebind(caster, target)) {
            return true;
        }
        Contract contract = clause.getCaster().findReferencedContract();
        return contract != null && RequestSystem.INSTANCE.submitAmend(contract, caster, ContractState.of(contract).withRebind(target));
    }
}
