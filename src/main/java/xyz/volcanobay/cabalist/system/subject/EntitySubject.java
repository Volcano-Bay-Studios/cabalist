package xyz.volcanobay.cabalist.system.subject;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;
import xyz.volcanobay.cabalist.core.CabalistAspects;
import xyz.volcanobay.cabalist.core.CabalistAttachments;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistDataComponents;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.blood.BloodStain;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.contract.Contractee;
import xyz.volcanobay.cabalist.system.contract.Term;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.energy.Lifeforce;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EntitySubject extends Subject implements Contractee {
    private static final double CONTRACT_REACH = 6;

    private final Entity entity;
    private final List<Contract> activeContracts = new ArrayList<>();

    public EntitySubject(Entity entity) {
        this.entity = entity;
    }

    public static EntitySubject of(Entity entity) {
        return entity.getData(CabalistAttachments.ENTITY_SUBJECT.get());
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public UUID getUUID() {
        return entity.getUUID();
    }

    @Override
    public boolean valid() {
        return entity.isAlive();
    }

    @Override
    public @Nullable BloodStain getBlood() {
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        BloodStain mainHand = living.getMainHandItem().get(CabalistDataComponents.BLOOD.get());
        return mainHand != null ? mainHand : living.getOffhandItem().get(CabalistDataComponents.BLOOD.get());
    }

    @Override
    public @Nullable Contract findReferencedContract() {
        if (entity instanceof LivingEntity living) {
            Contract held = ContractSystem.INSTANCE.getContract(living.getMainHandItem());
            if (held != null) {
                return held;
            }
        }
        HitResult hit = entity.pick(CONTRACT_REACH, 0, false);
        Contract aimedAt = hit instanceof BlockHitResult blockHit ? ContractBlockEntity.getContractAt(entity.level(), blockHit.getBlockPos()) : null;
        if (aimedAt != null) {
            return aimedAt;
        }
        for (UUID contractId : entity.getData(CabalistAttachments.CONTRACT_MEMBERSHIPS.get()).getContractIds()) {
            Contract contract = ContractSystem.INSTANCE.getContract(contractId);
            if (contract != null) {
                return contract;
            }
        }
        return null;
    }

    @Override
    public void onHangingSpellAdded() {
        entity.getData(CabalistAttachments.HANGING_SPELLS.get());
    }

    @Override
    public boolean isValid() {
        return entity.isAlive() && !entity.isRemoved();
    }

    @Override
    public void tickContract(Contract contract) {
    }

    @Override
    public List<Contract> getActiveContractsList() {
        return activeContracts;
    }

    @Override
    public void joinContract(Contract contract) {
        Contractee.super.joinContract(contract);
        entity.getData(CabalistAttachments.CONTRACT_MEMBERSHIPS.get()).add(contract.getUUID());
    }

    @Override
    public void leaveContract(Contract contract) {
        Contractee.super.leaveContract(contract);
        entity.getData(CabalistAttachments.CONTRACT_MEMBERSHIPS.get()).remove(contract.getUUID());
    }

    @Override
    public boolean receive(Aspect aspect, SpellClause clause, float magnitude) {
        return aspect.affectEntity(entity, clause, magnitude);
    }

    @Override
    public boolean represents(Entity entity) {
        return this.entity == entity;
    }

    @Override
    public double getLifeforcePool() {
        if (!(entity instanceof LivingEntity living)) {
            return 0;
        }
        return Math.max(0, Math.min(Lifeforce.get(living), living.getHealth() - CabalistAspects.HARM.get().getMinimumHealth())) * CabalistConfig.ENTROPY_PER_HEALTH.get();
    }

    @Override
    public double getEnergyContent() {
        if (!(entity instanceof LivingEntity living)) {
            return 0;
        }
        return living.getHealth();
    }

    @Override
    public EnergyStack getEnergyStack() {
        EnergyStack stack = entity.getData(CabalistAttachments.ENERGY_STACK.get());
        return stack;
    }

    @Override
    public boolean consents(Subject asker, Spell spell) {
        if (asker == this) {
            return true;
        }
        for (Set<Term> terms : getAllAppliedTerms().values()) {
            for (Term term : terms) {
                if (term.grantsConsent(spell)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Level getLevel() {
        return entity.level();
    }

    @Override
    public void getPosition(Vector3d out) {
        Vec3 position = entity.position();
        out.set(position.x, position.y, position.z);
    }

    @Override
    public void getFacing(Vector3d out) {
        Vec3 look = entity.getLookAngle();
        out.set(look.x, look.y, look.z);
    }

    @Override
    public void getCastOrigin(Vector3d out) {
        out.set(entity.getX(), entity.getEyeY(), entity.getZ());
    }

    @Override
    public @Nullable Subject getEnemy() {
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        LivingEntity enemy = living.getLastHurtByMob();
        if (enemy == null) {
            enemy = living.getLastHurtMob();
        }
        if (enemy == null) {
            return null;
        }
        return of(enemy);
    }

    @Override
    public Component getDisplayName() {
        return entity.getDisplayName();
    }
}
