package xyz.volcanobay.cabalist.system.barrier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.system.render.FormShape;

import java.util.List;
import java.util.UUID;

public record Barrier(FormShape.Kind kind, Vec3 start, Vec3 end, double radius, double height, int followId, List<UUID> allowed) {
    public static final double MIN_RADIUS = 2;
    public static final double AREA_DEPTH = 1;
    public static final double STRIKE_TAIL = 18;
    private static final double GRADIENT_STEP = 0.01;

    public static final Codec<Barrier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FormShape.Kind.CODEC.fieldOf("kind").forGetter(Barrier::kind),
            Vec3.CODEC.fieldOf("start").forGetter(Barrier::start),
            Vec3.CODEC.fieldOf("end").forGetter(Barrier::end),
            Codec.DOUBLE.fieldOf("radius").forGetter(Barrier::radius),
            Codec.DOUBLE.fieldOf("height").forGetter(Barrier::height),
            Codec.INT.fieldOf("follow").forGetter(Barrier::followId),
            UUIDUtil.CODEC.listOf().fieldOf("allowed").forGetter(Barrier::allowed)
    ).apply(instance, Barrier::new));

    public static Barrier of(FormShape shape, List<UUID> allowed) {
        return new Barrier(shape.kind(), shape.start(), shape.end(), Math.max(MIN_RADIUS, shape.radius()), shape.height(), shape.entityId(), allowed);
    }

    public boolean allows(Entity entity) {
        if (entity.getId() == followId || allowed.contains(entity.getUUID())) {
            return true;
        }
        return entity instanceof Projectile projectile && projectile.getOwner() != null && allows(projectile.getOwner());
    }

    public Barrier locate(Level level) {
        if (followId == FormShape.NO_ENTITY) {
            return this;
        }
        Entity followed = level.getEntity(followId);
        if (followed == null) {
            return this;
        }
        return switch (kind) {
            case BEAM -> {
                if (height <= 0) {
                    yield this;
                }
                Vec3 eyes = followed.getEyePosition();
                Vec3 aimed = eyes.add(followed.getViewVector(1).scale(height));
                Vec3 hit = level.clip(new ClipContext(eyes, aimed, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, followed)).getLocation();
                yield new Barrier(kind, eyes, hit, radius, height, followId, allowed);
            }
            case SKY_STRIKE -> {
                Vec3 bottom = followed.position();
                Vec3 top = new Vec3(bottom.x, Math.min(start.y, bottom.y + STRIKE_TAIL), bottom.z);
                yield new Barrier(kind, top, bottom, radius, height, followId, allowed);
            }
            default -> {
                Vec3 at = followed.position().add(0, followed.getBbHeight() / 2, 0);
                yield new Barrier(kind, at, at, radius, height, followId, allowed);
            }
        };
    }

    // basically an sdf for the barrier
    public double signedDistance(Vec3 at) {
        return switch (kind) {
            case AREA -> {
                double dx = at.x - start.x;
                double dz = at.z - start.z;
                double side = Math.sqrt(dx * dx + dz * dz) - radius;
                double below = start.y - AREA_DEPTH - at.y;
                double above = at.y - (start.y + height);
                yield Math.max(side, Math.max(below, above));
            }
            case BEAM, SKY_STRIKE -> distanceToSegment(at) - radius;
            case POINT, PROJECTILE -> at.distanceTo(start) - radius;
        };
    }

    public Vec3 clip(Vec3 from, Vec3 movement, double margin) {
        double before = signedDistance(from);
        if (!blocks(before, signedDistance(from.add(movement)), margin)) {
            return movement;
        }
        Vec3 normal = getNormal(from);
        Vec3 slid = movement.subtract(normal.scale(movement.dot(normal)));
        if (normal.lengthSqr() > 0 && !blocks(before, signedDistance(from.add(slid)), margin)) {
            return slid;
        }
        return Vec3.ZERO;
    }

    public @Nullable Vec3 deflect(Vec3 from, Vec3 velocity, double margin, double bounce) {
        double before = signedDistance(from);
        if (before < 0 || !blocks(before, signedDistance(from.add(velocity)), margin)) {
            return null;
        }
        Vec3 normal = getNormal(from);
        if (normal.lengthSqr() == 0) {
            return Vec3.ZERO;
        }
        Vec3 reflected = velocity.subtract(normal.scale(2 * velocity.dot(normal))).scale(bounce);
        return blocks(before, signedDistance(from.add(reflected)), margin) ? Vec3.ZERO : reflected;
    }

    private static boolean blocks(double before, double after, double margin) {
        if (before < 0) {
            return after > -margin && after > before;
        }
        return after < margin && after < before;
    }

    private Vec3 getNormal(Vec3 at) {
        double center = signedDistance(at);
        Vec3 gradient = new Vec3(signedDistance(at.add(GRADIENT_STEP, 0, 0)) - center, signedDistance(at.add(0, GRADIENT_STEP, 0)) - center,
                signedDistance(at.add(0, 0, GRADIENT_STEP)) - center);
        return gradient.lengthSqr() < 1e-12 ? Vec3.ZERO : gradient.normalize();
    }

    private double distanceToSegment(Vec3 at) {
        Vec3 along = end.subtract(start);
        double length = along.lengthSqr();
        double t = length < 1e-9 ? 0 : Math.max(0, Math.min(1, at.subtract(start).dot(along) / length));
        return at.distanceTo(start.add(along.scale(t)));
    }
}
