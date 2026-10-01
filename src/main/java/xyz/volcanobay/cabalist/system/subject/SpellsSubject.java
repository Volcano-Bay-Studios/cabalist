package xyz.volcanobay.cabalist.system.subject;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.spell.HangingSpellSystem;
import xyz.volcanobay.cabalist.system.spell.PendingSpell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

import java.util.List;

public class SpellsSubject extends Subject {
    private final Subject owner;

    public SpellsSubject(Subject owner) {
        this.owner = owner;
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
    public void collectMembers(SubjectList out) {
        for (PendingSpell pending : HangingSpellSystem.INSTANCE.get(owner)) {
            out.add(pending.getSpell());
        }
    }

    @Override
    public boolean receive(Aspect aspect, SpellClause clause, float magnitude) {
        boolean isAffected = false;
        for (PendingSpell pending : List.copyOf(HangingSpellSystem.INSTANCE.get(owner))) {
            isAffected |= pending.getSpell().receive(aspect, clause, magnitude);
        }
        return isAffected;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Spells upon ").append(owner.getDisplayName());
    }
}
