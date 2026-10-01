package xyz.volcanobay.cabalist.entity;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.content.spell.form.StrikeDelivery;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.spell.SpellExecutor;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.PositionSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.subject.SubjectList;

/**
 * A beam falling from the sky. It hits everything in its column and bores through blocks,
 * losing pierce power by each solid block's hardness until it stops
 */
public class FallingBeam extends Entity {
    private static final double PIERCE_POWER_PER_MAGNITUDE = 6;
    private static final int MAX_LIFETIME = 200;
    private static final Vec3 DOWN = new Vec3(0, -1, 0);

    private final IntOpenHashSet hitEntities = new IntOpenHashSet();
    private final SubjectList hits = new SubjectList();
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private @Nullable SpellClause clause;
    private @Nullable StrikeDelivery delivery;
    private double speed;
    private double radius;
    private double remainingDistance;
    private double piercePower;
    private int nextLayer = Integer.MAX_VALUE;
    private int surfaceLayer = Integer.MIN_VALUE;
    private int centerSurfaceLayer = Integer.MIN_VALUE;

    public FallingBeam(EntityType<? extends FallingBeam> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void launch(SpellClause clause, Vec3 top, double speed, double radius, double maxDistance) {
        this.clause = clause;
        this.speed = speed;
        this.radius = radius;
        this.remainingDistance = maxDistance;
        this.piercePower = clause.getPower() * PIERCE_POWER_PER_MAGNITUDE;
        setPos(top);
        setDeltaMovement(DOWN.scale(speed));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
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
            finishAtSurface(position);
            return;
        }

        hits.clear();
        AABB column = new AABB(position.x - radius, next.y, position.z - radius, position.x + radius, position.y, position.z + radius);
        for (Entity entity : level().getEntities(this, column)) {
            double dx = entity.getX() - position.x;
            double dz = entity.getZ() - position.z;
            if (dx * dx + dz * dz <= radius * radius && hitEntities.add(entity.getId()) && !clause.getLocation().represents(entity)) {
                hits.add(EntitySubject.of(entity));
            }
        }
        if (!hits.isEmpty()) {
            SpellExecutor.INSTANCE.apply(clause, hits);
        }
        if (clause.isExhausted()) {
            discard();
            return;
        }

        int top =Math.min(nextLayer, (int) Math.floor(position.y));
        int bottom = (int) Math.floor(next.y);
        for (int layer = top; layer >= bottom; layer--) {
            nextLayer = layer - 1;
            if (!pierceLayer(position, layer)) {
                finishAtSurface(new Vec3(position.x, layer + 1, position.z));
                return;
            }
        }
        remainingDistance -= speed;
        setPos(next);
    }

    private boolean pierceLayer(Vec3 center, int layer) {
        hits.clear();
        int reach = (int) Math.ceil(radius);
        int centerX = (int) Math.floor(center.x);
        int centerZ = (int) Math.floor(center.z);
        int columns = 0;
        double hardness = 0;
        boolean isBlocked = false;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                columns++;
                cursor.set(centerX + dx, layer, centerZ + dz);
                BlockState state = level().getBlockState(cursor);
                if (state.isAir()) {
                    continue;
                }
                if (surfaceLayer == Integer.MIN_VALUE) {
                    surfaceLayer = layer;
                }
                if (dx == 0 && dz == 0 && centerSurfaceLayer == Integer.MIN_VALUE) {
                    centerSurfaceLayer = layer;
                }
                hits.add(PositionSubject.ofBlock(level(), cursor));
                if (!state.blocksMotion()) {
                    continue;
                }
                float destroySpeed = state.getDestroySpeed(level(), cursor);
                if (destroySpeed < 0) {
                    isBlocked = true;
                } else {
                    hardness += destroySpeed;
                }
            }
        }
        if (!hits.isEmpty() && clause != null) {
            SpellExecutor.INSTANCE.apply(clause, hits);
        }
        piercePower -= hardness / Math.max(columns, 1);
        return !isBlocked && piercePower > 0 && (clause == null || !clause.isExhausted());
    }

    private void finishAtSurface(Vec3 fallback) {
        Vec3 position = position();
        if (centerSurfaceLayer != Integer.MIN_VALUE) {
            cursor.set(position.x, centerSurfaceLayer, position.z);
            finish(new Vec3(position.x, centerSurfaceLayer + 1, position.z), PositionSubject.ofBlock(level(), cursor));
        } else if (surfaceLayer != Integer.MIN_VALUE) {
            finish(new Vec3(position.x, surfaceLayer + 1, position.z), null);
        } else {
            finish(fallback, null);
        }
    }

    public void setDelivery(StrikeDelivery delivery) {
        this.delivery = delivery;
    }

    private void finish(Vec3 at, @Nullable Subject hit) {
        discard();
        if (delivery != null) {
            delivery.land(at, hit);
        }
    }
}
