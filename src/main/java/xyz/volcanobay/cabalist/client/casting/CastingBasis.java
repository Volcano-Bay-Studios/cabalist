package xyz.volcanobay.cabalist.client.casting;

import net.minecraft.world.phys.Vec3;

public record CastingBasis(Vec3 origin, Vec3 forward, Vec3 right, Vec3 up) {
    private static final Vec3 WORLD_UP = new Vec3(0, 1, 0);

    public static CastingBasis of(Vec3 origin, Vec3 forward) {
        Vec3 right = forward.cross(WORLD_UP);
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        return new CastingBasis(origin, forward, right, right.cross(forward).normalize());
    }

    public Vec3 offset(float x, float y, float d) {
        return forward.scale(d).add(right.scale(x)).add(up.scale(y));
    }
}
