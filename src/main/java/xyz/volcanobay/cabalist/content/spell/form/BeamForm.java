package xyz.volcanobay.cabalist.content.spell.form;

import net.minecraft.world.level.Level;
import xyz.volcanobay.cabalist.system.form.Form;
import xyz.volcanobay.cabalist.system.form.FormDimension;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.spell.SpellExecutor;

/**
 * Hits everything along the line from the caster until it meets a block.
 */
public class BeamForm extends Form {
    private static final double LENGTH = 32;
    private static final double WIDTH = 0.3;

    @Override
    public double getCostFactor(SpellClause clause) {
        return getDimension(clause, FormDimension.SIZE, LENGTH) / LENGTH * getDimension(clause, FormDimension.BREADTH, WIDTH) / WIDTH;
    }

    @Override
    public boolean completesLater() {
        return true;
    }

    @Override
    public int deliver(SpellClause clause, SpellExecutor executor) {
        Level level = clause.getLocation().getLevel();
        if (level == null) {
            return 0;
        }
        BeamDelivery delivery = new BeamDelivery(clause, level, getDimension(clause, FormDimension.SIZE, LENGTH), getDimension(clause, FormDimension.BREADTH, WIDTH));
        delivery.showVisual(delivery.getShape());
        delivery.start();
        return 0;
    }
}
