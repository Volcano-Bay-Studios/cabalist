package xyz.volcanobay.cabalist.content.spell.component;

import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;

// Pushes the spell off its caster, so what it sets going carries on after they're gone
public class ReleaseComponent extends SpellComponent {

    @Override
    public SpellRole getRole() {
        return SpellRole.MANNER;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        word.getClause().setReleased(true);
    }
}
