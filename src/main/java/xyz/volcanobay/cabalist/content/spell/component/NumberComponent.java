package xyz.volcanobay.cabalist.content.spell.component;

import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;

/**
 * numbers are recognized by the engine rather than a dictionary. The value is stored on the word.
 */
public class NumberComponent extends SpellComponent {

    @Override
    public SpellRole getRole() {
        return SpellRole.NUMBER;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
    }
}
