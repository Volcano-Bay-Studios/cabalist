package xyz.volcanobay.cabalist.system.rift;

import org.joml.Vector3d;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.Subject;

/**
 * A clause with no form reaches a target other than its caster through a rift opened only for that one spell.
 * The cost payed "reduces the distance", so all distance goes through {@link #getEffectiveDistance}.
 */
public class RiftHelper {
    private static final Vector3d FROM = new Vector3d();
    private static final Vector3d TO = new Vector3d();

    public static boolean opensRift(SpellClause clause) {
        Subject target = clause.getTarget();
        return clause.getForm() == null && target != null && target != clause.getCaster();
    }

    public static double getEffectiveDistance(Subject from, Subject to) {
        if (from.getLevel() == null || to.getLevel() == null) {
            return 0;
        }
        if (from.getLevel() != to.getLevel()) {
            return CabalistConfig.CROSS_DIMENSION_DISTANCE.get();
        }
        from.getCastOrigin(FROM);
        to.getPosition(TO);
        return FROM.distance(TO);
    }

    public static double getRiftCost(SpellClause clause, double energyMoved) {
        Subject target = clause.getTarget();
        if (!opensRift(clause) || target == null) {
            return 0;
        }
        return getEffectiveDistance(clause.getLocation(), target) * energyMoved * CabalistConfig.RIFT_COST_PER_BLOCK.get();
    }
}
