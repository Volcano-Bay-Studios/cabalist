package xyz.volcanobay.cabalist.content.spell.component;

import xyz.volcanobay.cabalist.core.CabalistAspects;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;

// The new name is everything said after it, or the quoted text
public class RenameComponent extends SpellComponent {

    @Override
    public SpellRole getRole() {
        return SpellRole.PETITION;
    }

    @Override
    public boolean claimsTrailingText() {
        return true;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        word.getClause().addAspect(CabalistAspects.RENAME_CONTRACT.get());
        word.getClause().setContractName(word.getTrailingText());
    }
}
