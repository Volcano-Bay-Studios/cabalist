package xyz.volcanobay.cabalist.content.spell.component;

import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;

import java.util.function.Supplier;

public class AspectComponent extends SpellComponent {
    private final Supplier<? extends Aspect> aspect;

    public AspectComponent(Supplier<? extends Aspect> aspect) {
        this.aspect = aspect;
    }

    @Override
    public SpellRole getRole() {
        return SpellRole.PETITION;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        word.getClause().addAspect(aspect.get());
    }
}
