package xyz.volcanobay.cabalist.content.spell.component;

import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;

public class ColorComponent extends SpellComponent {
    private final int color;

    public ColorComponent(int color) {
        this.color = 0xFF000000 | color;
    }

    @Override
    public SpellRole getRole() {
        return SpellRole.MANNER;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        word.getClause().addColor(color);
    }
}
