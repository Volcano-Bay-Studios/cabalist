package xyz.volcanobay.cabalist.content.spell.component;

import xyz.volcanobay.cabalist.system.form.FormDimension;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;

/**
 * "Wide", "long", "narrow", etc. sets a form dimension to a spoken number, or scales it without one.
 */
public class FormSizeComponent extends SpellComponent {
    private static final SpellRole[] NEEDS = { SpellRole.NUMBER};

    private final FormDimension dimension;
    private final double factor;

    public FormSizeComponent(FormDimension dimension, double factor) {
        this.dimension = dimension;
        this.factor = factor;
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
        Word number = word.getClaimed(SpellRole.NUMBER);
        if (number != null) {
            word.getClause().setFormDimension(dimension, number.getNumber());
            return;
        }
        word.getClause().multiplyFormFactor(dimension, factor);
    }
}
