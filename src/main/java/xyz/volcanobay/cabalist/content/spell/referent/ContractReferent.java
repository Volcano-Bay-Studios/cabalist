package xyz.volcanobay.cabalist.content.spell.referent;

import xyz.volcanobay.cabalist.system.subject.Subject;

public class ContractReferent extends OwnedReferent {
    @Override
    protected Subject getOwned(Subject owner) {
        return owner.findReferencedContract();
    }
}
