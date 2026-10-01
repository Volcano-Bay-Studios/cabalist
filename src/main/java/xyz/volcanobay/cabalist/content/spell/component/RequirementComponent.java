package xyz.volcanobay.cabalist.content.spell.component;

import xyz.volcanobay.cabalist.system.contract.Term;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;

import java.util.function.Supplier;

public class RequirementComponent extends SpellComponent {
    private final Supplier<Term> requirement;

    public RequirementComponent(Supplier<Term> requirement) {
        this.requirement = requirement;
    }

    @Override
    public SpellRole getRole() {
        return SpellRole.CONDITION;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        word.getClause().addRequirement(requirement.get().create(), resolver.getSpokenText(word));
    }
}
