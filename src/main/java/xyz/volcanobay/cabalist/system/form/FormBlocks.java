package xyz.volcanobay.cabalist.system.form;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.subject.PositionSubject;
import xyz.volcanobay.cabalist.system.subject.SubjectList;

import java.util.Arrays;

public class FormBlocks {
    public static final int NO_SURFACE = Integer.MIN_VALUE;
    private static final double LINE_STEP = 0.2;

    private static final BlockPos.MutableBlockPos CURSOR = new BlockPos.MutableBlockPos();

    public static void collectAlongLine(Level level, Vec3 start, Vec3 end, SubjectList out) {
        LongOpenHashSet visited = new LongOpenHashSet();
        Vec3 direction = end.subtract(start);
        double length = direction.length();
        for (double travelled = 0; travelled <= length; travelled += LINE_STEP) {
            double fraction = length == 0 ? 0 : travelled / length;
            addIfNotAir(level, start.x + direction.x * fraction, start.y + direction.y * fraction, start.z + direction.z * fraction, visited, out);
        }
        addIfNotAir(level, end.x, end.y, end.z, visited, out);
    }

    private static void addIfNotAir(Level level, double x, double y, double z, LongOpenHashSet visited, SubjectList out) {
        CURSOR.set(x, y, z);
        if (visited.add(CURSOR.asLong()) && !level.getBlockState(CURSOR).isAir()) {
            out.add(PositionSubject.ofBlock(level, CURSOR));
        }
    }

    /**
     * Adds the topmost non-air block of every column in the cylinder, and returns the top of each column's solid ground.
     */
    public static int[] collectSurface(Level level, Vector3d center, double radius, double halfHeight, SubjectList out) {
        int reach = (int) Math.ceil(radius);
        int size = reach * 2 + 1;
        int[] groundTops = new int[size * size];
        Arrays.fill(groundTops, NO_SURFACE);
        int centerX = (int) Math.floor(center.x);
        int centerZ = (int) Math.floor(center.z);
        int top = (int) Math.floor(center.y + halfHeight);
        int bottom = (int) Math.floor(center.y - halfHeight);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                boolean isSurfaceFound = false;
                for (int y = top; y >= bottom; y--) {
                    CURSOR.set(centerX + dx, y, centerZ + dz);
                    BlockState state = level.getBlockState(CURSOR);
                    if (state.isAir()) {
                        continue;
                    }
                    if (!isSurfaceFound) {
                        out.add(PositionSubject.ofBlock(level, CURSOR));
                        isSurfaceFound = true;
                    }
                    if (state.blocksMotion()) {
                        groundTops[(dx + reach) * size + (dz + reach)] = y + 1;
                        break;
                    }
                }
            }
        }
        return groundTops;
    }

    public static int getColumnIndex(Vector3d center, double radius, double x, double z) {
        int reach = (int) Math.ceil(radius);
        int size = reach * 2 + 1;
        int dx = (int) Math.floor(x) - (int) Math.floor(center.x);
        int dz = (int) Math.floor(z) - (int) Math.floor(center.z);
        if (Math.abs(dx) > reach || Math.abs(dz) > reach) {
            return -1;
        }
        return (dx + reach) * size + (dz + reach);
    }
}
