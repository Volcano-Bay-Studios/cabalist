package xyz.volcanobay.cabalist.client.renderer.form;

import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.client.Camera;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircle;
import xyz.volcanobay.cabalist.client.spell.ActiveVisual;

import java.util.List;

public class ProjectileRenderer extends FormRenderer {
    private static final float MIN_SIZE = 0.25f;
    private static final float SPIN = 0.3f;
    private static final float FLOW = 0.1f;

    @Override
    public boolean needsDefaultLayer() {
        return true;
    }

    @Override
    public double getVolume(ActiveVisual visual) {
        return 1;
    }

    @Override
    public void samplePoint(ActiveVisual visual, RandomSource random, float partialTick, Vector3d out) {
        Vec3 at = visual.getFollowedPosition(partialTick);
        double size = Math.max(visual.getShape().radius(), MIN_SIZE);
        out.set(at.x + (random.nextDouble() - 0.5) * size, at.y + (random.nextDouble() - 0.5) * size, at.z + (random.nextDouble() - 0.5) * size);
    }

    @Override
    public void collectCircles(ActiveVisual visual, float partialTick, List<MagicCircle> out) {
        Entity entity = visual.getEntity();
        Vec3 heading = entity == null ? Vec3.ZERO : entity.getDeltaMovement();
        heading = heading.lengthSqr() < 1.0e-6 ? MagicCircle.UP : heading.normalize();
        out.add(new MagicCircle(visual.getFollowedPosition(partialTick), heading, Math.max(visual.getShape().radius(), MIN_SIZE) * 2.5f));
    }

    @Override
    public void buildLayer(ActiveVisual visual, VertexConsumer buffer, MatrixStack pose, Camera camera, float partialTick, int color) {
        Vec3 at = visual.getFollowedPosition(partialTick);
        float halfSize = Math.max(visual.getShape().radius(), MIN_SIZE);
        Entity entity = visual.getEntity();
        Vec3 heading = entity == null ? Vec3.ZERO : entity.getDeltaMovement();
        heading = heading.lengthSqr() < 1.0e-6 ? new Vec3(0, 1, 0) : heading.normalize();
        Vec3 back = at.subtract(heading.scale(halfSize));
        Vec3 front = at.add(heading.scale(halfSize));
        layeredFrustum(visual, partialTick, buffer, pose, relativeTo(camera, back.x, back.y, back.z), relativeTo(camera, front.x, front.y, front.z),
                halfSize, halfSize, color, SPIN, FLOW);
    }
}
