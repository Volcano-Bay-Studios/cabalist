package xyz.volcanobay.cabalist.system.subject;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

import java.util.UUID;

/**
 * Points at whoever the blood came from. Blood given willingly carries their consent.
 */
public class BloodSubject extends Subject {
    private final EntitySubject owner;
    private final boolean consented;

    public BloodSubject(EntitySubject owner, boolean consented) {
        this.owner = owner;
        this.consented = consented;
    }

    @Override
    public @Nullable Level getLevel() {
        return owner.getLevel();
    }

    @Override
    public void getPosition(Vector3d out) {
        owner.getPosition(out);
    }

    @Override
    public void getFacing(Vector3d out) {
        owner.getFacing(out);
    }

    @Override
    public void getCastOrigin(Vector3d out) {
        owner.getCastOrigin(out);
    }

    @Override
    public boolean receive(Aspect aspect, SpellClause clause, float magnitude) {
        return owner.receive(aspect, clause, magnitude);
    }

    @Override
    public boolean represents(Entity entity) {
        return owner.represents(entity);
    }

    @Override
    public boolean consents(Subject asker, Spell spell) {
        return consented || owner.consents(asker, spell);
    }

    @Override
    public @Nullable UUID getUUID() {
        return owner.getUUID();
    }

    @Override
    public boolean isValid() {
        return owner.isValid();
    }

    @Override
    public @Nullable EnergyStack getEnergyStack() {
        return owner.getEnergyStack();
    }

    @Override
    public double getLifeforcePool() {
        return owner.getLifeforcePool();
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Blood of ").append(owner.getDisplayName());
    }
}
