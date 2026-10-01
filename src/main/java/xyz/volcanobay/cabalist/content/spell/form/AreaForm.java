package xyz.volcanobay.cabalist.content.spell.form;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.form.Form;
import xyz.volcanobay.cabalist.system.form.FormDimension;
import xyz.volcanobay.cabalist.system.render.FormShape;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.spell.SpellExecutor;

/**
 * A cylinder. The surface blocks within it, and the entities standing above that surface.
 */
public class AreaForm extends Form {
    private static final double RADIUS = 4;
    private static final double MAX_RADIUS = 32;
    private static final double MIN_HEIGHT = 3;
    private static final double MAX_HEIGHT = 64;

    private static double getRadius(SpellClause clause) {
        return Math.min(MAX_RADIUS,getDimension(clause, FormDimension.SIZE, RADIUS) * clause.getFormFactor(FormDimension.BREADTH));
    }

    private static double getHeight(SpellClause clause, double radius) {
        return Mth.clamp(getDimension(clause, FormDimension.HEIGHT, Math.max(MIN_HEIGHT, radius) / Math.max(clause.getPower(), 0.01f)), 1, MAX_HEIGHT);
    }

    @Override
    public double getCostFactor(SpellClause clause) {
        double ratio = getRadius(clause) / RADIUS;
        return ratio * ratio;
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
        Vector3d center = new Vector3d();
        getCenter(clause, center);
        boolean centeredOnLocation = clause.getTarget() == null && clause.getOrigin() == null;
        double radius = getRadius(clause);
        AreaDelivery delivery = new AreaDelivery(clause, level, center, radius, centeredOnLocation);
        Vec3 at = new Vec3(center.x, center.y, center.z);
        delivery.showVisual(new FormShape(FormShape.Kind.AREA, at, at, (float) radius, (float) getHeight(clause, radius), AreaDelivery.getExpandTicks(radius), FormShape.NO_ENTITY));
        delivery.start();
        return 0;
    }
}
