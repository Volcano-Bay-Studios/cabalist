package xyz.volcanobay.cabalist.content.spell.component;

import xyz.volcanobay.cabalist.content.term.RequirementTerm;
import xyz.volcanobay.cabalist.core.CabalistTerms;
import xyz.volcanobay.cabalist.system.contract.Term;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;

// Waits on the death of whatever it's said of, like "when my enemy dies", or the host's own without one
public class DiesComponent extends SpellComponent {
    private static final SpellRole[] NEEDS = {SpellRole.REFERENT};

    @Override
    public SpellRole getRole() {
        return SpellRole.CONDITION;
    }

    @Override
    public SpellRole[] getNeeds() {
        return NEEDS;
    }

    // After the referents, so "my enemy" is whole before it's claimed.
    @Override
    public int getPriority() {
        return -10;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        Term term = CabalistTerms.DIES.get().create();
        Word about = word.getClaimed(SpellRole.REFERENT);
        String phrase = resolver.getSpokenText(word);
        if (about != null && term instanceof RequirementTerm requirement) {
            if (about.getSubject() != null) {
                requirement.watch(about.getSubject());
            } else {
                requirement.watchNothing();
            }
            phrase = resolver.getSpokenText(about) + " " + phrase;
        }
        word.getClause().addRequirement(term, phrase);
    }
}
