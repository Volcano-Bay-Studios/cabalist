package xyz.volcanobay.cabalist.content.spell.component;

import xyz.volcanobay.cabalist.core.CabalistTerms;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;

/**
 * Between two requirements it combines them; anywhere else "and" and "then" split clauses.
 */
public class Conjunction extends SpellComponent {
    private final Kind kind;

    public Conjunction(Kind kind) {
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public SpellRole getRole() {
        return SpellRole.CONJUNCTION;
    }

    @Override
    public boolean splitsClauses() {
        return kind != Kind.EITHER;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        SpellClause clause = word.getClause();
        if (word.joinsRequirements()) {
            if (kind == Kind.EITHER) {
                clause.startRequirementGroup();
            }
            return;
        }
        if (kind == Kind.AFTER && clause.getPrevious() != null) {
            clause.addCondition(CabalistTerms.AFTER_PREVIOUS.get().create());
        }
    }

    public enum Kind {
        TOGETHER,
        AFTER,
        EITHER
    }
}
