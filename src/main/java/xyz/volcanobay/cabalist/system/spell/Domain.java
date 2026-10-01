package xyz.volcanobay.cabalist.system.spell;

import xyz.volcanobay.cabalist.system.aspect.EnergyUse;

public class Domain extends SpellComponent {

    public Domain() {
        super();
    }

    @Override
    public SpellRole getRole() {
        return SpellRole.INVOCATION;
    }

    /**
     * Domains only affect energy, never what a spell does.
     */
    public void modifyEnergyUse(EnergyUse use, SpellClause clause) {
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        if (word.getClause().getDomain() == null) {
            word.getClause().setDomain(this);
        }
        word.getClause().getDevotion().addDomainWord(this, word.getPhrase(), word.getStrength());
    }
}
