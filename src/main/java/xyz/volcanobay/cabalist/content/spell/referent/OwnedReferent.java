package xyz.volcanobay.cabalist.content.spell.referent;

import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;
import xyz.volcanobay.cabalist.system.subject.Subject;

// Something belonging to another referent, like "their enemy", or to the caster when said alone.
public abstract class OwnedReferent extends SpellComponent {
    private static final SpellRole[] NEEDS = {SpellRole.REFERENT};

    protected abstract @Nullable Subject getOwned(Subject owner);

    @Override
    public SpellRole getRole() {
        return SpellRole.REFERENT;
    }

    @Override
    public SpellRole[] getNeeds() {
        return NEEDS;
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        Word owner = word.getClaimed(SpellRole.REFERENT);
        Subject ownerSubject = owner != null && owner.getSubject() != null ? owner.getSubject() : word.getClause().getCaster();
        word.setSubject(getOwned(ownerSubject));
    }
}
