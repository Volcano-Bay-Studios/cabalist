package xyz.volcanobay.cabalist.content.spell.form;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.entity.FallingBeam;
import xyz.volcanobay.cabalist.system.form.Delivery;
import xyz.volcanobay.cabalist.system.form.FormBlocks;
import xyz.volcanobay.cabalist.system.render.FormShape;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.PositionSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.subject.SubjectList;

/**
 * Falls until the beam stops, then stands as a column from the sky to where it landed,
 * touching everything* inside it and the ground beneath it.
 */
public class StrikeDelivery extends Delivery {
    private static final double GROUND_DEPTH = 1;

    private final FallingBeam strike;
    private final double radius;
    private final double height;
    private final double topY;
    private boolean isLanded;
    private Vec3 landedAt = Vec3.ZERO;
    private @Nullable Subject hit;

    public StrikeDelivery(SpellClause clause, Level level, FallingBeam strike, double radius, double height) {
        super(clause, level);
        this.strike = strike;
        this.radius = radius;
        this.height = height;
        this.topY = strike.getY();
    }

    public FormShape getFallingShape() {
        return FormShape.following(FormShape.Kind.SKY_STRIKE, strike.getId(), strike.position(), (float) radius, (float) height);
    }

    public void land(Vec3 at, @Nullable Subject hit) {
        if (isLanded) {
            return;
        }
        isLanded = true;
        this.landedAt = at;
        this.hit = hit;
        updateVisual(0, new FormShape(FormShape.Kind.SKY_STRIKE, new Vec3(at.x, topY, at.z), at, (float) radius, (float) height, 0, FormShape.NO_ENTITY));
    }

    @Override
    protected boolean deliverTick() {
        if (!isLanded && strike.isRemoved()) {
            land(strike.position(), null);
        }
        return isLanded;
    }

    @Override
    protected void collectSustained(SubjectList out) {
        Vector3d ground = new Vector3d(landedAt.x, landedAt.y, landedAt.z);
        FormBlocks.collectSurface(level, ground, radius, GROUND_DEPTH, out);
        AABB column = new AABB(landedAt.x - radius, landedAt.y - GROUND_DEPTH, landedAt.z - radius, landedAt.x + radius, topY, landedAt.z + radius);
        for (Entity entity : level.getEntities((Entity) null, column)) {
            double dx = entity.getX() - landedAt.x;
            double dz = entity.getZ() - landedAt.z;
            if (dx * dx + dz * dz <= radius * radius && !clause.getLocation().represents(entity)) {
                out.add(EntitySubject.of(entity));
            }
        }
    }

    @Override
    protected Subject getEndLocation() {
        return new PositionSubject(level, new Vector3d(landedAt.x, landedAt.y, landedAt.z), new Vector3d(0, -1, 0));
    }

    @Override
    protected @Nullable Subject getEndHit() {
        return hit;
    }

    @Override
    public void end() {
        if (!strike.isRemoved()) {
            strike.discard();
        }
        super.end();
    }
}
