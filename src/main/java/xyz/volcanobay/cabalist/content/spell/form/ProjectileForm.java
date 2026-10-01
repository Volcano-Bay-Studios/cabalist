package xyz.volcanobay.cabalist.content.spell.form;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.core.CabalistEntities;
import xyz.volcanobay.cabalist.entity.SpellProjectile;
import xyz.volcanobay.cabalist.system.form.Form;
import xyz.volcanobay.cabalist.system.form.FormDimension;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.spell.SpellExecutor;

/**
 * Launches a projectile from the caster toward the target, or along the caster's facing. It lingers on what it hits.
 */
public class ProjectileForm extends Form {
    private static final double RANGE = 32;
    private static final double HIT_RADIUS = 0.3;
    private static final double SPEED = 1.5;

    @Override
    public double getCostFactor(SpellClause clause) {
        return getDimension(clause, FormDimension.SIZE, RANGE) / RANGE * getDimension(clause, FormDimension.BREADTH, HIT_RADIUS) / HIT_RADIUS;
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
        double range = getDimension(clause, FormDimension.SIZE, RANGE);
        Vector3d from = new Vector3d();
        Vector3d to = new Vector3d();
        aim(clause, from, to, range);
        to.sub(from).normalize(SPEED);

        double hitRadius = getDimension(clause, FormDimension.BREADTH, HIT_RADIUS);
        SpellProjectile projectile = new SpellProjectile(CabalistEntities.SPELL_PROJECTILE.get(), level);
        ProjectileDelivery delivery = new ProjectileDelivery(clause, level, projectile, hitRadius);
        projectile.launch(clause, delivery, new Vec3(from.x, from.y, from.z), new Vec3(to.x, to.y, to.z), range, hitRadius);
        level.addFreshEntity(projectile);
        delivery.showVisual(delivery.getFlightShape());
        delivery.start();
        return 0;
    }
}
