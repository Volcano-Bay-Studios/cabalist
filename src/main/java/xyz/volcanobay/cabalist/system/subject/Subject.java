package xyz.volcanobay.cabalist.system.subject;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.blood.BloodStain;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

import java.util.UUID;

/**
 * Anything that can cast, bear, or be targeted by a spell. Every subject is a group, usually a group of one.
 */
public abstract class Subject {
    public abstract @Nullable Level getLevel();

    public abstract void getPosition(Vector3d out);

    public abstract void getFacing(Vector3d out);

    public void getCastOrigin(Vector3d out) {
        getPosition(out);
    }

    public abstract boolean receive(Aspect aspect, SpellClause clause, float magnitude);

    public boolean represents(Entity entity) {
        return false;
    }

    public void collectMembers(SubjectList out) {
        out.add(this);
    }

    /**
     * Null if the subject has no stack of its own and pulls straight from the world.
     */
    public @Nullable EnergyStack getEnergyStack() {
        return null;
    }

    public @Nullable BloodStain getBlood() {
        return null;
    }

    public @Nullable Contract getBoundContract() {
        return null;
    }

    public @Nullable Contract findReferencedContract() {
        return getBoundContract();
    }

    public void onHangingSpellAdded() {
    }

    public boolean isValid() {
        return true;
    }

    public @Nullable UUID getUUID() {
        return null;
    }

    public double getLifeforcePool() {
        return 0;
    }

    public double getEnergyContent() {
        return 0;
    }

    public boolean consents(Subject asker, Spell spell) {
        return asker == this;
    }

    public @Nullable Subject getEnemy() {
        return null;
    }

    public abstract Component getDisplayName();
}