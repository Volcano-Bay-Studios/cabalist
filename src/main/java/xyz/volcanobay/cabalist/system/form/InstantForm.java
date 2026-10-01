package xyz.volcanobay.cabalist.system.form;

import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.spell.SpellExecutor;
import xyz.volcanobay.cabalist.system.subject.SubjectList;

public abstract class InstantForm extends Form {
    public abstract void collectSubjects(SpellClause clause, SubjectList out);

    @Override
    public int deliver(SpellClause clause, SpellExecutor executor) {
        return executor.deliverInstantly(clause, this);
    }
}
