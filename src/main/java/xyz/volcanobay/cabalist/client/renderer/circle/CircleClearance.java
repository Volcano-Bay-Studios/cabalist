package xyz.volcanobay.cabalist.client.renderer.circle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

// Circles rise out of blocks they'd be lodged in. Ones that can show through blocks only keep out of the floor.
public class CircleClearance {
    private static final int RIM_SAMPLES = 12;
    private static final float RIM_INSET = 0.8f;
    private static final int GRAZE_SAMPLES = 1;
    private static final double STEP = 0.1;
    private static final double MAX_LIFT = 2;

    private static final double RISE_PER_SECOND = 5;
    private static final double SINK_PER_SECOND = 1.5;
    private static final long SINK_DELAY_NANOS = 800_000_000L;
    private static final long FORGET_NANOS = 2_000_000_000L;

    private static final Map<Object, Eased> EASED = new HashMap<>();
    private static long lastPruned;

    private static class Eased {
        private double lift;
        private double held;
        private long clearSince;
        private long at;
    }

    public static Vec3 lift(Object key, Vec3 center, Vec3 normal, float radius, boolean isFloorOnly) {
        double target = getLift(center, normal, radius, isFloorOnly);
        long now = System.nanoTime();
        Eased eased = EASED.get(key);
        if (eased == null) {
            eased = new Eased();
            eased.lift = target;
            eased.held = target;
            EASED.put(key, eased);
        } else {
            if (target >= eased.held) {
                eased.held = target;
                eased.clearSince = 0;
            } else if (eased.clearSince == 0) {
                eased.clearSince = now;
            } else if (now - eased.clearSince > SINK_DELAY_NANOS) {
                eased.held = target;
                eased.clearSince = 0;
            }
            double seconds = Math.max(0, (now - eased.at) / 1e9);
            double rate = eased.held > eased.lift ? RISE_PER_SECOND : SINK_PER_SECOND;
            eased.lift += (eased.held - eased.lift) * (1 - Math.exp(-seconds * rate));
        }
        eased.at = now;
        if (now - lastPruned > FORGET_NANOS) {
            lastPruned = now;
            EASED.values().removeIf(old -> now - old.at > FORGET_NANOS);
        }
        return center.add(0, eased.lift, 0);
    }

    private static double getLift(Vec3 center, Vec3 normal, float radius, boolean isFloorOnly) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return 0;
        }
        MagicCircle circle = new MagicCircle(center, normal, radius);
        Vec3 right = circle.getRight();
        Vec3 up = circle.getUp();
        for (double lift = 0; lift <= MAX_LIFT; lift += STEP) {
            if (!isBlocked(level, center.add(0, lift, 0), right, up, radius, isFloorOnly)) {
                return lift;
            }
        }
        return 0;
    }

    // Sampled a little inside the rim, and a edge contact samples doesn't count, so brushing an edge isn't being lodged in it.
    private static boolean isBlocked(ClientLevel level, Vec3 center, Vec3 right, Vec3 up, float radius, boolean isFloorOnly) {
        if (!isFloorOnly && isInside(level, center)) {
            return true;
        }
        float inset = radius * RIM_INSET;
        int inside = 0;
        for (int i = 0; i < RIM_SAMPLES; i++) {
            float angle = Mth.TWO_PI * i / RIM_SAMPLES;
            Vec3 point = center.add(right.scale(Mth.cos(angle) * inset)).add(up.scale(Mth.sin(angle) * inset));
            if ((!isFloorOnly || point.y <= center.y) && isInside(level, point) && ++inside > GRAZE_SAMPLES) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInside(ClientLevel level, Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (shape.isEmpty()) {
            return false;
        }
        Vec3 local = point.subtract(pos.getX(), pos.getY(), pos.getZ());
        return shape.bounds().contains(local);
    }
}
