package xyz.volcanobay.cabalist.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.content.spell.form.ProjectileDelivery;
import xyz.volcanobay.cabalist.system.form.FormBlocks;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.spell.SpellExecutor;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.PositionSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.subject.SubjectList;

/**
 * Carries a clause through the air and tells its delivery where it landed.
 */
public class SpellProjectile extends Projectile {
    private static final int MAX_LIFETIME = 200;

    private final SubjectList hits = new SubjectList();
    private @Nullable SpellClause clause;
    private @Nullable ProjectileDelivery delivery;
    private double remainingDistance;
    private double hitRadius;

    public SpellProjectile(EntityType<? extends SpellProjectile> type, Level level) {
        super(type, level);
    }

    public void launch(SpellClause clause, ProjectileDelivery delivery, Vec3 position, Vec3 velocity, double maxDistance, double hitRadius) {
        this.clause = clause;
        this.delivery = delivery;
        this.remainingDistance = maxDistance;
        this.hitRadius = hitRadius;
        setPos(position);
        setDeltaMovement(velocity);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!super.canHitEntity(entity)) {
            return false;
        }
        return clause == null || !clause.getLocation().represents(entity);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 position = position();
        Vec3 next = position.add(getDeltaMovement());
        if (level().isClientSide) {
            setPos(next);
            return;
        }
        if (clause == null) {
            discard();
            return;
        }
        if (tickCount > MAX_LIFETIME || remainingDistance <= 0) {
            finish(position, null);
            return;
        }

        BlockHitResult blockHit = level().clip(new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? next : blockHit.getLocation();
        AABB swept = getBoundingBox().expandTowards(end.subtract(position)).inflate(hitRadius);

        hits.clear();
        Vec3 pathEnd = blockHit.getType() == HitResult.Type.MISS ? end : end.subtract(getDeltaMovement().normalize().scale(0.05));
        FormBlocks.collectAlongLine(level(), position, pathEnd, hits);
        if (!hits.isEmpty()) {
            SpellExecutor.INSTANCE.apply(clause, hits);
        }
        if (clause.isExhausted()) {
            discard();
            return;
        }

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, position, end, swept, this::canHitEntity);
        if (entityHit != null) {
            EntitySubject hit = EntitySubject.of(entityHit.getEntity());
            hits.clear();
            hits.add(hit);
            SpellExecutor.INSTANCE.apply(clause, hits);
            finish(entityHit.getLocation(), hit);
            return;
        }

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            PositionSubject hit = PositionSubject.ofBlock(level(), blockHit.getBlockPos());
            hits.clear();
            hits.add(hit);
            SpellExecutor.INSTANCE.apply(clause, hits);
            finish(blockHit.getLocation().subtract(getDeltaMovement().normalize().scale(0.1)), hit);
            return;
        }
        remainingDistance -= getDeltaMovement().length();
        setPos(next);
    }

    private void finish(Vec3 at, @Nullable Subject hit) {
        discard();
        if (delivery != null) {
            delivery.land(at, hit, getDeltaMovement().normalize());
        }
    }
}
