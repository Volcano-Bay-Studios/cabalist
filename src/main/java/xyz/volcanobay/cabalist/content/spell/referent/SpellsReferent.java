package xyz.volcanobay.cabalist.content.spell.referent;

import xyz.volcanobay.cabalist.system.subject.SpellsSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

public class SpellsReferent extends OwnedReferent {
    @Override
    protected Subject getOwned(Subject owner) {
        return new SpellsSubject(owner);
    }
}
