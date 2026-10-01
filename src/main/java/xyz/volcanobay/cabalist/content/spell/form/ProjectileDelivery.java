package xyz.volcanobay.cabalist.content.spell.form;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.entity.SpellProjectile;
import xyz.volcanobay.cabalist.system.form.Delivery;
import xyz.volcanobay.cabalist.system.render.FormShape;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.PositionSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.subject.SubjectList;

/**
 * Flies until the projectile lands, and then lingers on whatever it hit. A projectile that hits nothing ends where it stops.
 */
public class ProjectileDelivery extends Delivery {
    private static final float LINGER_RADIUS = 0.5f;

    private final SpellProjectile projectile;
    private final double hitRadius;
    private boolean isLanded;
    private Vec3 landedAt = Vec3.ZERO;
    private Vec3 heading = new Vec3(0, -1, 0);
    private @Nullable Subject hit;

    public ProjectileDelivery(SpellClause clause, Level level, SpellProjectile projectile, double hitRadius) {
        super(clause, level);
        this.projectile = projectile;
        this.hitRadius = hitRadius;
    }

    public FormShape getFlightShape() {
        return FormShape.following(FormShape.Kind.PROJECTILE, projectile.getId(), projectile.position(), (float) hitRadius, 0);
    }

    public void land(Vec3 at, @Nullable Subject hit, Vec3 heading) {
        if (isLanded) {
            return;
        }
        isLanded = true;
        this.landedAt = at;
        this.hit = hit;
        this.heading = heading;
        if (hit instanceof EntitySubject entitySubject) {
            updateVisual(0, FormShape.following(FormShape.Kind.POINT, entitySubject.getEntity().getId(), entitySubject.getEntity().position(), LINGER_RADIUS, 0));
        } else {
            updateVisual(0, FormShape.point(at, 0));
        }
    }

    @Override
    protected boolean deliverTick() {
        if (!isLanded && projectile.isRemoved()) {
            land(projectile.position(), null, projectile.getDeltaMovement().normalize());
        }
        return isLanded;
    }

    // A projectile spends itself on impact rather than clinging to what it hit.
    @Override
    protected void collectSustained(SubjectList out) {
    }

    @Override
    protected boolean hasSustainedSubjects() {
        return false;
    }

    @Override
    protected Subject getEndLocation() {
        return new PositionSubject(level, new Vector3d(landedAt.x, landedAt.y, landedAt.z), new Vector3d(heading.x, heading.y, heading.z));
    }

    @Override
    protected @Nullable Subject getEndHit() {
        return hit;
    }

    @Override
    public void end() {
        if (!projectile.isRemoved()) {
            projectile.discard();
        }
        super.end();
    }
}
