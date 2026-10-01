package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.contract.DraftSystem;
import xyz.volcanobay.cabalist.system.contract.HostTarget;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.PositionSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.UUID;

// Lays a contract out as the caster's own draft to change. Aimed at something without one, it starts a draft for a new contract there.
public class OpenDraftAspect extends ContractAspect {

    @Override
    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        UUID caster = clause.getCaster().getUUID();
        if (caster == null) {
            return false;
        }
        DraftSystem.INSTANCE.open(caster, contract);
        return true;
    }

    @Override
    public boolean affectPosition(Level level, BlockPos pos, SpellClause clause, float magnitude) {
        Contract contract = ContractBlockEntity.getContractAt(level, pos);
        if (contract != null) {
            return affectContract(contract, clause, magnitude);
        }
        return openNew(PositionSubject.ofBlock(level, pos), clause);
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        if (entity instanceof LivingEntity living) {
            Contract contract = ContractSystem.INSTANCE.getContract(living.getMainHandItem());
            if (contract != null) {
                return affectContract(contract, clause, magnitude);
            }
        }
        return openNew(EntitySubject.of(entity), clause);
    }

    private static boolean openNew(Subject target, SpellClause clause) {
        UUID caster = clause.getCaster().getUUID();
        HostTarget createAt = HostTarget.of(target);
        if (caster == null || createAt == null) {
            return false;
        }
        DraftSystem.INSTANCE.openNew(caster, createAt);
        return true;
    }
}
