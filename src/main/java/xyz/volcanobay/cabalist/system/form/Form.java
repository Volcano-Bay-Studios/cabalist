package xyz.volcanobay.cabalist.system.form;

import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.spell.SpellExecutor;
import xyz.volcanobay.cabalist.system.subject.Subject;

/**
 * Pure geometry and timing: how a clause reaches its subjects. Forms know nothing about what happens to them.
 * Sizes are spoken numbers when given, otherwise a default scaled by size words and spell power.
 */
public abstract class Form {
    /**
     * Returns how many subjects were affected immediately; timed forms return 0 and apply later.
     */
    public abstract int deliver(SpellClause clause, SpellExecutor executor);

    public double getCostFactor(SpellClause clause) {
        return 1;
    }

    public boolean completesLater() {
        return false;
    }

    protected static double getDimension(SpellClause clause, FormDimension dimension, double base) {
        double spoken = clause.getFormDimension(dimension);
        if (!Double.isNaN(spoken)) {
            return spoken;
        }
        return base * clause.getFormFactor(dimension) * clause.getPower() * clause.getDevotion().getSizeMultiplier(clause.getPrimaryDomain());
    }

    public static void aim(SpellClause clause, Vector3d from, Vector3d to, double range) {
        Subject location = clause.getLocation();
        location.getCastOrigin(from);
        Subject target = clause.getTarget();
        if (target != null && target != location) {
            target.getCastOrigin(to);
            return;
        }
        location.getFacing(to);
        to.mul(range).add(from);
    }

    protected static void getCenter(SpellClause clause, Vector3d out) {
        Subject center = clause.getTarget();
        if (center == null) {
            center = clause.getOrigin();
        }
        if (center == null) {
            center = clause.getLocation();
        }
        center.getPosition(out);
    }
}
