package xyz.volcanobay.cabalist.content.spell.component;

import xyz.volcanobay.cabalist.system.form.Form;
import xyz.volcanobay.cabalist.system.form.FormDimension;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;

import java.util.function.Supplier;

public class FormComponent extends SpellComponent {
    private static final SpellRole[] NEEDS = { SpellRole.NUMBER };

    private final Supplier<Form> form;

    public FormComponent(Supplier<Form> form) {
        this.form = form;
    }

    @Override
    public SpellRole getRole() {
        return SpellRole.MANNER;
    }

    @Override
    public SpellRole[] getNeeds() {
        return NEEDS;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        if (word.getClause().getForm() == null) {
            word.getClause().setForm(form.get());
        }
        Word number = word.getClaimed(SpellRole.NUMBER);
        if (number != null) {
            word.getClause().setFormDimension(FormDimension.SIZE, number.getNumber());
        }
    }
}
