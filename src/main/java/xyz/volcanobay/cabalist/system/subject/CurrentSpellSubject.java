package xyz.volcanobay.cabalist.system.subject;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

/**
 * The subject point at the spell.
 */
public class CurrentSpellSubject extends Subject {
    private final SpellClause clause;

    public CurrentSpellSubject(SpellClause clause) {
        this.clause = clause;
    }

    private Subject getAnchor() {
        Spell spell = clause.getSpell();
        return spell == null ? clause.getCaster() : spell;
    }

    @Override
    public @Nullable Level getLevel() {
        return getAnchor().getLevel();
    }

    @Override
    public void getPosition(Vector3d out) {
        getAnchor().getPosition(out);
    }

    @Override
    public void getFacing(Vector3d out) {
        getAnchor().getFacing(out);
    }

    @Override
    public void collectMembers(SubjectList out) {
        Spell spell = clause.getSpell();
        if (spell != null) {
            out.add(spell);
        }
    }

    @Override
    public boolean receive(Aspect aspect, SpellClause clause, float magnitude) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("This spell");
    }
}
