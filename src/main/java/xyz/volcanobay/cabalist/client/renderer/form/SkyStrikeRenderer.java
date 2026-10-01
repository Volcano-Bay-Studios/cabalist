package xyz.volcanobay.cabalist.client.renderer.form;

import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircle;
import xyz.volcanobay.cabalist.client.spell.ActiveVisual;
import xyz.volcanobay.cabalist.system.barrier.Barrier;
import xyz.volcanobay.cabalist.util.ColorHelper;

import java.util.List;

// A falling column that trails up behind the strike, thinning and fading toward the sky, so it reads as dropping rather than growing
public class SkyStrikeRenderer extends FormRenderer {
    private static final float SPIN = 0.05f;
    private static final float FLOW = 0.2f;
    private static final float SOURCE_WIDTH = 0.15f;
    private static final int TAIL_SEGMENTS = 6;

    public static Vec3 getBottom(ActiveVisual visual, float partialTick) {
        return visual.getShape().followsEntity() ? visual.getFollowedPosition(partialTick) : visual.getShape().end();
    }

    public static double getTop(ActiveVisual visual, Vec3 bottom) {
        return Math.min(visual.getShape().start().y, bottom.y + Barrier.STRIKE_TAIL);
    }

    @Override
    public boolean needsDefaultLayer() {
        return true;
    }

    @Override
    public double getVolume(ActiveVisual visual) {
        double radius = visual.getShape().radius();
        return Math.PI * radius * radius * visual.getShape().height();
    }

    @Override
    public void samplePoint(ActiveVisual visual, RandomSource random, float partialTick, Vector3d out) {
        Vec3 bottom = getBottom(visual, partialTick);
        double top = getTop(visual, bottom);
        randomInDisc(random, visual.getShape().radius(), out);
        out.add(bottom.x, bottom.y + random.nextDouble() * Math.max(0, top - bottom.y), bottom.z);
    }

    @Override
    public void collectCircles(ActiveVisual visual, float partialTick, List<MagicCircle> out) {
        Vec3 bottom = getBottom(visual, partialTick);
        float radius = visual.getShape().radius() * 1.6f;
        out.add(new MagicCircle(new Vec3(bottom.x, visual.getShape().start().y, bottom.z), MagicCircle.DOWN, radius));
        if (!visual.getShape().followsEntity()) {
            out.add(new MagicCircle(bottom.add(0, MagicCircle.GROUND_OFFSET, 0), MagicCircle.UP, radius));
        }
    }

    @Override
    public void buildLayer(ActiveVisual visual, VertexConsumer buffer, MatrixStack pose, Camera camera, float partialTick, int color) {
        Vec3 bottomWorld = getBottom(visual, partialTick);
        double topY = getTop(visual, bottomWorld);
        if (topY <= bottomWorld.y) {
            return;
        }
        float halfWidth = visual.getShape().radius();
        float alpha = ColorHelper.getAlpha(color);
        for (int i = 0; i < TAIL_SEGMENTS; i++) {
            double lower = bottomWorld.y + (topY - bottomWorld.y) * i / TAIL_SEGMENTS;
            double upper = bottomWorld.y + (topY - bottomWorld.y) * (i + 1) / TAIL_SEGMENTS;
            Vector3f bottom = relativeTo(camera, bottomWorld.x, lower, bottomWorld.z);
            Vector3f top = relativeTo(camera, bottomWorld.x, upper, bottomWorld.z);
            float fade = 1 - (float) i / TAIL_SEGMENTS;
            float lowerWidth = halfWidth * Mth.lerp((float) i / TAIL_SEGMENTS, 1, SOURCE_WIDTH);
            float upperWidth = halfWidth * Mth.lerp((float) (i + 1) / TAIL_SEGMENTS, 1, SOURCE_WIDTH);
            layeredFrustum(visual, partialTick, buffer, pose, top, bottom, upperWidth, lowerWidth, ColorHelper.withAlpha(color, alpha * fade * fade), SPIN, FLOW);
        }
    }
}
