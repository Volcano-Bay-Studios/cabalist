package xyz.volcanobay.cabalist.content.spell.component;

import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;

import java.util.function.Supplier;

public class DevotionComponent extends SpellComponent {
    private final Supplier<Domain> domain;

    public DevotionComponent(Supplier<Domain> domain) {
        this.domain = domain;
    }

    @Override
    public SpellRole getRole() {
        return SpellRole.DEVOTIONAL;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        word.getClause().getDevotion().addPhrase(domain.get(), word.getPhrase(), word.getStrength());
    }
}
