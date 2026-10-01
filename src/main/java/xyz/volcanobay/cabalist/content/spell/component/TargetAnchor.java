package xyz.volcanobay.cabalist.content.spell.component;

import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;

public class TargetAnchor extends SpellComponent {
    private static final SpellRole[] NEEDS = {SpellRole.REFERENT};

    @Override
    public SpellRole getRole() {
        return SpellRole.ANCHOR;
    }

    @Override
    public SpellRole[] getNeeds() {
        return NEEDS;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        Word referent = word.getClaimed(SpellRole.REFERENT);
        if (referent != null && referent.getSubject() != null) {
            word.getClause().setTarget(referent.getSubject());
        }
    }
}
