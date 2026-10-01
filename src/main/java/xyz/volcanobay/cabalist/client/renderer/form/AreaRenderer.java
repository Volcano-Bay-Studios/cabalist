package xyz.volcanobay.cabalist.client.renderer.form;

import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.client.Camera;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircle;
import xyz.volcanobay.cabalist.client.spell.ActiveVisual;

import java.util.List;

public class AreaRenderer extends FormRenderer {
    private static final float SURFACE_OFFSET = 0.05f;
    private static final float HEIGHT = 1.5f;
    private static final float TOP_SCALE = 0.8f;
    private static final float SPIN = 0.01f;
    private static final float FLOW = 0.05f;
    private static final ShellStyle SHELL_STYLE = new ShellStyle(0.15f, 0.7f, false);

    private static double getCurrentRadius(ActiveVisual visual, float partialTick) {
        return visual.getShape().radius() * visual.getProgress(partialTick);
    }

    @Override
    public double getVolume(ActiveVisual visual) {
        double radius = visual.getShape().radius();
        return Math.PI * radius * radius;
    }

    @Override
    public void samplePoint(ActiveVisual visual, RandomSource random, float partialTick, Vector3d out) {
        Vec3 center = visual.getShape().start();
        randomInDisc(random, getCurrentRadius(visual, partialTick), out);
        out.add(center.x, center.y + SURFACE_OFFSET, center.z);
    }

    @Override
    public void collectCircles(ActiveVisual visual, float partialTick, List<MagicCircle> out) {
        float radius = (float) getCurrentRadius(visual, partialTick);
        if (radius > 0) {
            out.add(new MagicCircle(visual.getShape().start().add(0, MagicCircle.GROUND_OFFSET, 0), MagicCircle.UP, radius));
            if (BarrierShellRenderer.isBarrier(visual)) {
                out.add(new MagicCircle(visual.getShape().start().add(0, visual.getShape().height(), 0), MagicCircle.DOWN, radius));
            }
        }
    }

    @Override
    public void buildLayer(ActiveVisual visual, VertexConsumer buffer, MatrixStack pose, Camera camera, float partialTick, int color) {
        Vec3 centerWorld = visual.getShape().start();
        float radius = (float) getCurrentRadius(visual, partialTick);
        if (radius <= 0) {
            return;
        }
        Vector3f base = relativeTo(camera, centerWorld.x, centerWorld.y + SURFACE_OFFSET, centerWorld.z);
        Vector3f top = relativeTo(camera, centerWorld.x, centerWorld.y + SURFACE_OFFSET + HEIGHT, centerWorld.z);
        layeredFrustum(visual, partialTick, buffer, pose, base, top, radius, radius * TOP_SCALE, color, SPIN, FLOW, SHELL_STYLE);
    }
}
