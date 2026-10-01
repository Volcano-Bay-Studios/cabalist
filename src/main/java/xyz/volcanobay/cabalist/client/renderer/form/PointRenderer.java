package xyz.volcanobay.cabalist.client.renderer.form;

import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircle;
import xyz.volcanobay.cabalist.client.spell.ActiveVisual;

import java.util.List;

/**
 * A spinning cube around what the spell is working on, sized to fit an entity it follows.
 */
public class PointRenderer extends FormRenderer {
    private static final float ENTITY_MARGIN = 0.6f;
    private static final float BOB_SPEED = 0.1f;
    private static final float BOB_HEIGHT = 0.05f;
    private static final float SPIN = 0.04f;
    private static final float FLOW = 0.03f;

    private static Vec3 getCenter(ActiveVisual visual, float partialTick) {
        Entity entity = visual.getEntity();
        Vec3 at = visual.getFollowedPosition(partialTick);
        double lift = entity == null ? 0 : entity.getBbHeight() / 2;
        return at.add(0, lift + Mth.sin(visual.getTime(partialTick) * BOB_SPEED) * BOB_HEIGHT, 0);
    }

    private static float getHalfSize(ActiveVisual visual) {
        Entity entity = visual.getEntity();
        if (entity == null) {
            return visual.getShape().radius();
        }
        return Math.max(visual.getShape().radius(), Math.max(entity.getBbWidth(), entity.getBbHeight()) * ENTITY_MARGIN);
    }

    @Override
    public double getVolume(ActiveVisual visual) {
        double size = getHalfSize(visual) * 2;
        return Math.max(1, size * size * size);
    }

    @Override
    public void samplePoint(ActiveVisual visual, RandomSource random, float partialTick, Vector3d out) {
        Vec3 at = getCenter(visual, partialTick);
        double spread = getHalfSize(visual) * 2;
        out.set(at.x + (random.nextDouble() - 0.5) * spread, at.y + (random.nextDouble() - 0.5) * spread, at.z + (random.nextDouble() - 0.5) * spread);
    }

    @Override
    public void collectCircles(ActiveVisual visual, float partialTick, List<MagicCircle> out) {
        Entity entity = visual.getEntity();
        Vec3 at = visual.getFollowedPosition(partialTick);
        float radius = entity == null ? visual.getShape().radius() * 2 : Math.max(entity.getBbWidth() * 1.2f, 0.8f);
        double lift = entity == null ? -visual.getShape().radius() : 0;
        out.add(new MagicCircle(at.add(0, lift + MagicCircle.GROUND_OFFSET, 0), MagicCircle.UP, radius));
    }

    @Override
    public void buildLayer(ActiveVisual visual, VertexConsumer buffer, MatrixStack pose, Camera camera, float partialTick, int color) {
        Vec3 at = getCenter(visual, partialTick);
        float halfSize = getHalfSize(visual);
        layeredFrustum(visual, partialTick, buffer, pose, relativeTo(camera, at.x, at.y - halfSize, at.z), relativeTo(camera, at.x, at.y + halfSize, at.z),
                halfSize, halfSize, color, SPIN, FLOW);
    }
}
