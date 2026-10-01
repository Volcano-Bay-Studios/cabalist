package xyz.volcanobay.cabalist.content.spell.referent;

import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;
import xyz.volcanobay.cabalist.system.subject.WorldSubject;

public class WorldReferent extends SpellComponent {

    @Override
    public SpellRole getRole() {
        return SpellRole.REFERENT;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        word.setSubject(new WorldSubject(word.getClause().getCaster().getLevel()));
    }
}
