package xyz.volcanobay.cabalist.client.renderer.circle;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Occlusion {
    public static boolean isBlocked(Vec3 from, Vec3 look, double distance) {
        Entity viewer = Minecraft.getInstance().getCameraEntity();
        if (viewer == null) {
            return false;
        }
        HitResult hit = viewer.level().clip(new ClipContext(from, from.add(look.scale(distance)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, viewer));
        return hit.getType() != HitResult.Type.MISS;
    }
}
