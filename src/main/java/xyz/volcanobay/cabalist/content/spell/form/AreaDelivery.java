package xyz.volcanobay.cabalist.content.spell.form;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.form.FormBlocks;
import xyz.volcanobay.cabalist.system.form.Delivery;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.PositionSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.subject.SubjectList;

import java.util.Arrays;

/**
 * Expands from the center, reaching surface blocks by distance with some randomness,
 * and entities above the surface as the edge passes them. Once open, every pulse reaches the whole cylinder.
 */
public class AreaDelivery extends Delivery {
    private static final int TICKS_PER_BLOCK = 2;
    private static final int MAX_JITTER_TICKS = 4;

    private final Vector3d center;
    private final double radius;
    private final boolean sparesLocation;
    private final int expandTicks;
    private final int[] groundTops;
    private final Subject[] blocks;
    private final int[] arrivals;
    private final IntOpenHashSet hitEntities = new IntOpenHashSet();
    private final Vector3d blockPosition = new Vector3d();
    private int nextBlock;

    public AreaDelivery(SpellClause clause, Level level, Vector3d center, double radius, boolean sparesLocation) {
        super(clause, level);
        this.center = center;
        this.radius = radius;
        this.sparesLocation = sparesLocation;
        this.expandTicks = getExpandTicks(radius);

        SubjectList surface = new SubjectList();
        groundTops = FormBlocks.collectSurface(level, center, radius, radius, surface);
        RandomSource random = level.getRandom();
        long[] order = new long[surface.size()];
        for (int i = 0; i < surface.size(); i++) {
            surface.get(i).getPosition(blockPosition);
            double dx = blockPosition.x - center.x;
            double dz = blockPosition.z - center.z;
            int arrival = (int) (Math.sqrt(dx * dx + dz * dz) / Math.max(radius, 0.001) * expandTicks) + random.nextInt(MAX_JITTER_TICKS + 1);
            order[i] = ((long) arrival << 32) | i;
        }
        Arrays.sort(order);
        blocks = new Subject[order.length];
        arrivals = new int[order.length];
        for (int i = 0; i < order.length; i++) {
            arrivals[i] = (int) (order[i] >> 32);
            blocks[i] = surface.get((int) order[i]);
        }
    }

    public static int getExpandTicks(double radius) {
        return Math.max(1, (int) Math.ceil(radius * TICKS_PER_BLOCK)) + MAX_JITTER_TICKS;
    }

    @Override
    protected boolean deliverTick() {
        while (nextBlock < blocks.length && arrivals[nextBlock] <= age) {
            batch.add(blocks[nextBlock]);
            nextBlock++;
        }
        collectEntities(Math.min(radius, radius * age / expandTicks), batch, true);
        applyBatch();
        return age >= expandTicks && nextBlock >= blocks.length;
    }

    @Override
    protected void collectSustained(SubjectList out) {
        FormBlocks.collectSurface(level, center, radius, radius, out);
        collectEntities(radius, out, false);
    }

    private void collectEntities(double currentRadius, SubjectList out, boolean onlyNew) {
        AABB area = new AABB(center.x - currentRadius, center.y - radius, center.z - currentRadius,
                center.x + currentRadius, center.y + radius, center.z + currentRadius);
        Subject location = clause.getLocation();
        for (Entity entity : level.getEntities((Entity) null, area)) {
            if ((onlyNew && hitEntities.contains(entity.getId())) || (sparesLocation && location.represents(entity))) {
                continue;
            }
            double dx = entity.getX() - center.x;
            double dz = entity.getZ() - center.z;
            if (dx * dx + dz * dz > currentRadius * currentRadius) {
                continue;
            }
            int column = FormBlocks.getColumnIndex(center, radius, entity.getX(), entity.getZ());
            if (column >= 0 && groundTops[column] != FormBlocks.NO_SURFACE && entity.getY() < groundTops[column] - 0.01) {
                continue;
            }
            hitEntities.add(entity.getId());
            out.add(EntitySubject.of(entity));
        }
    }

    @Override
    protected Subject getEndLocation() {
        return new PositionSubject(level, center, new Vector3d(0, 1, 0));
    }
}
