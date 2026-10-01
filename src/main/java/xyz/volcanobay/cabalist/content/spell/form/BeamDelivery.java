package xyz.volcanobay.cabalist.content.spell.form;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.form.Delivery;
import xyz.volcanobay.cabalist.system.form.Form;
import xyz.volcanobay.cabalist.system.form.FormBlocks;
import xyz.volcanobay.cabalist.system.render.FormShape;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.PositionSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.subject.SubjectList;

import java.util.Arrays;

/**
 * Extends from its start, affecting blocks and entities as its tip passes them.
 */
public class BeamDelivery extends Delivery {
    private static final double BLOCKS_PER_TICK = 4;
    private static final int MAX_JITTER_TICKS = 1;
    private static final double VISUAL_MOVE_THRESHOLD = 0.05;

    private static final double MAX_RANGE = 128;
    private static final double MAX_WIDTH = 5;

    private final double range;
    private final double width;
    private final Subject[] blocks;
    private final int[] arrivals;
    private final IntOpenHashSet hitEntities = new IntOpenHashSet();
    private final Vector3d from = new Vector3d();
    private final Vector3d to = new Vector3d();
    private Vec3 start;
    private Vec3 end;
    private Vec3 direction;
    private double length;
    private @Nullable Subject endHit;
    private Vec3 shownStart;
    private Vec3 shownEnd;
    private int nextBlock;
    private double reach;

    public BeamDelivery(SpellClause clause, Level level, double range, double width) {
        super(clause, level);
        this.range = Math.min(MAX_RANGE,range);
        this.width = Math.min(MAX_WIDTH,width);
        aim();
        this.shownStart = start;
        this.shownEnd = end;

        SubjectList line = new SubjectList();
        collectLineBlocks(line);
        RandomSource random = level.getRandom();
        Vector3d blockPosition = new Vector3d();
        long[] order = new long[line.size()];
        for (int i = 0; i < line.size(); i++) {
            line.get(i).getPosition(blockPosition);
            double distance = Math.sqrt(blockPosition.distanceSquared(start.x, start.y, start.z));
            int arrival = (int) (distance / BLOCKS_PER_TICK) + random.nextInt(MAX_JITTER_TICKS + 1);
            order[i] = ((long) arrival << 32) | i;
        }
        Arrays.sort(order);
        blocks = new Subject[order.length];
        arrivals = new int[order.length];
        for (int i = 0; i < order.length; i++) {
            arrivals[i] = (int) (order[i] >> 32);
            blocks[i] = line.get((int) order[i]);
        }
    }

    private void aim() {
        Form.aim(clause, from, to, range);
        to.sub(from).normalize(range).add(from);
        start = new Vec3(from.x, from.y, from.z);
        Vec3 aimEnd = new Vec3(to.x, to.y, to.z);
        BlockHitResult blockHit = level.clip(new ClipContext(start, aimEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        end = blockHit.getLocation();
        direction = aimEnd.subtract(start).normalize();
        length = end.distanceTo(start);
        endHit = blockHit.getType() == HitResult.Type.BLOCK ? PositionSubject.ofBlock(level, BlockPos.containing(end.add(direction.scale(0.05)))) : null;
    }

    private void collectLineBlocks(SubjectList out) {
        FormBlocks.collectAlongLine(level, start, endHit != null ? end.add(direction.scale(0.05)) : end, out);
    }

    public FormShape getShape() {
        int channeler = FormShape.NO_ENTITY;
        float facingReach = 0;
        if (clause.getLocation() instanceof EntitySubject entitySubject) {
            channeler = entitySubject.getEntity().getId();
            Subject target = clause.getTarget();
            if (target == null || target == clause.getLocation()) {
                facingReach = (float) range;
            }
        }
        return new FormShape(FormShape.Kind.BEAM, start, end, (float) width, facingReach, getExtendTicks(length), channeler);
    }

    public static int getExtendTicks(double length) {
        return Math.max(1, (int) Math.ceil(length / BLOCKS_PER_TICK));
    }

    @Override
    protected boolean deliverTick() {
        double previousReach = reach;
        reach = Math.min(length, reach + BLOCKS_PER_TICK);
        while (nextBlock < blocks.length && arrivals[nextBlock] <= age) {
            batch.add(blocks[nextBlock]);
            nextBlock++;
        }
        collectEntities(start.add(direction.scale(previousReach)), start.add(direction.scale(reach)), batch, true);
        applyBatch();
        return reach >= length && nextBlock >= blocks.length;
    }

    @Override
    protected void sustainTick() {
        aim();
        if (start.distanceToSqr(shownStart) > VISUAL_MOVE_THRESHOLD * VISUAL_MOVE_THRESHOLD
                || end.distanceToSqr(shownEnd) > VISUAL_MOVE_THRESHOLD * VISUAL_MOVE_THRESHOLD) {
            shownStart = start;
            shownEnd = end;
            updateVisual(0, getShape());
        }
    }

    @Override
    protected void collectSustained(SubjectList out) {
        collectLineBlocks(out);
        collectEntities(start, end, out, false);
    }

    private void collectEntities(Vec3 lineFrom, Vec3 lineTo, SubjectList out, boolean onlyNew) {
        Subject location = clause.getLocation();
        for (Entity entity : level.getEntities((Entity) null, new AABB(lineFrom, lineTo).inflate(width + 1))) {
            if ((onlyNew && hitEntities.contains(entity.getId())) || location.represents(entity)) {
                continue;
            }
            if (entity.getBoundingBox().inflate(width).clip(lineFrom, lineTo).isPresent()) {
                hitEntities.add(entity.getId());
                out.add(EntitySubject.of(entity));
            }
        }
    }

    @Override
    protected boolean hasSustainedSubjects() {
        return clause.getLocation().isValid();
    }

    @Override
    protected Subject getEndLocation() {
        Vec3 at = endHit != null ? end.subtract(direction.scale(0.1)) : end;
        return new PositionSubject(level, new Vector3d(at.x, at.y, at.z), new Vector3d(direction.x, direction.y, direction.z));
    }

    @Override
    protected @Nullable Subject getEndHit() {
        return endHit;
    }
}
