package xyz.volcanobay.cabalist.client.renderer.circle;

import net.minecraft.world.phys.Vec3;

public record MagicCircle(Vec3 center, Vec3 normal, float radius) {
    public static final Vec3 UP = new Vec3(0, 1, 0);
    public static final Vec3 DOWN = new Vec3(0, -1, 0);
    public static final double GROUND_OFFSET = 0.03;
    public static final int SEED_MASK = 511;
    private static final Vec3 EAST = new Vec3(1, 0, 0);

    public Vec3 getRight() {
        Vec3 unit = normal.normalize();
        return unit.cross(Math.abs(unit.y) < 0.99 ? UP : EAST).normalize();
    }

    public Vec3 getUp() {
        return getRight().cross(normal.normalize()).normalize();
    }

    public double getHitDistance(Vec3 from, Vec3 look) {
        double facing = look.dot(normal);
        if (Math.abs(facing) < 1e-4) {
            return -1;
        }
        double distance = center.subtract(from).dot(normal) / facing;
        boolean isHit = distance > 0 && from.add(look.scale(distance)).distanceToSqr(center) <= radius * radius;
        return isHit ? distance : -1;
    }
}
