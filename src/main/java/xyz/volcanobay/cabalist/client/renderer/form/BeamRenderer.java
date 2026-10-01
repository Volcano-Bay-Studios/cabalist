package xyz.volcanobay.cabalist.client.renderer.form;

import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircle;
import xyz.volcanobay.cabalist.client.spell.ActiveVisual;

import java.util.List;

public class BeamRenderer extends FormRenderer {
    private static final float MIN_WIDTH = 0.25f;
    private static final float SPIN = 0.08f;
    private static final float FLOW = -0.15f;

    private static final double HAND_FORWARD = 0.55;
    private static final double HAND_SIDE = 0.28;
    private static final double HAND_DROP = 0.18;
    private static final double ARMS_FORWARD = 0.8;
    private static final double ARMS_DROP = 0.35;
    private static final float HAND_TAPER = 0.25f;

    public static Vec3 getStart(ActiveVisual visual, float partialTick) {
        Entity channeler = visual.getEntity();
        if (channeler == null) {
            return visual.getStart(partialTick);
        }
        Vec3 eyes = channeler.getEyePosition(partialTick);
        Vec3 forward = channeler.getViewVector(partialTick);
        if (!isFirstPersonChanneler(channeler)) {
            return eyes.add(forward.scale(ARMS_FORWARD)).subtract(0, ARMS_DROP, 0);
        }
        Vec3 right = forward.cross(MagicCircle.UP).normalize();
        Vec3 up = right.cross(forward).normalize();
        double side = channeler instanceof Player player && player.getMainArm() == HumanoidArm.LEFT ? -1 : 1;
        return eyes.add(forward.scale(HAND_FORWARD)).add(right.scale(HAND_SIDE * side)).subtract(up.scale(HAND_DROP));
    }

    private static boolean isFirstPersonChanneler(@Nullable Entity channeler) {
        Minecraft minecraft = Minecraft.getInstance();
        return channeler != null && channeler == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
    }

    public static Vec3 getEnd(ActiveVisual visual, float partialTick) {
        Entity channeler = visual.getEntity();
        float reach = visual.getShape().height();
        if (channeler == null || reach <= 0) {
            return visual.getEnd(partialTick);
        }
        Vec3 eyes = channeler.getEyePosition(partialTick);
        Vec3 aimed = eyes.add(channeler.getViewVector(partialTick).scale(reach));
        return channeler.level().clip(new ClipContext(eyes, aimed, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, channeler)).getLocation();
    }

    private static Vec3 getTip(ActiveVisual visual, float partialTick) {
        Vec3 start = getStart(visual, partialTick);
        return start.add(getEnd(visual, partialTick).subtract(start).scale(visual.getProgress(partialTick)));
    }

    private static float getHalfWidth(ActiveVisual visual) {
        return Math.max(visual.getShape().radius(), MIN_WIDTH);
    }

    @Override
    public boolean needsDefaultLayer() {
        return true;
    }

    @Override
    public double getVolume(ActiveVisual visual) {
        double width = getHalfWidth(visual);
        return visual.getShape().start().distanceTo(visual.getShape().end()) * Math.PI * width * width;
    }

    @Override
    public void samplePoint(ActiveVisual visual, RandomSource random, float partialTick, Vector3d out) {
        Vec3 start = getStart(visual, partialTick);
        Vec3 tip = getTip(visual, partialTick);
        double along = random.nextDouble();
        double width = getHalfWidth(visual);
        out.set(start.x + (tip.x - start.x) * along + (random.nextDouble() - 0.5) * width,
                start.y + (tip.y - start.y) * along + (random.nextDouble() - 0.5) * width,
                start.z + (tip.z - start.z) * along + (random.nextDouble() - 0.5) * width);
    }

    @Override
    public void collectCircles(ActiveVisual visual, float partialTick, List<MagicCircle> out) {
        Vec3 start = getStart(visual, partialTick);
        Vec3 tip = getTip(visual, partialTick);
        Vec3 direction = tip.subtract(start);
        if (direction.lengthSqr() < 1.0e-6) {
            return;
        }
        direction = direction.normalize();
        float width = getHalfWidth(visual);
        // In first person a full circle at the hand would fill the view.
//        if (isFirstPersonChanneler(visual.getEntity())) {
            out.add(new MagicCircle(start.add(direction.scale(width)), direction, Math.max(width * (isFirstPersonChanneler(visual.getEntity()) ? 1 : 3), 0.7f)));
//        }
        out.add(new MagicCircle(tip, direction, Math.max(width * 2, 0.5f)));
    }

    @Override
    public void buildLayer(ActiveVisual visual, VertexConsumer buffer, MatrixStack pose, Camera camera, float partialTick, int color) {
        Vec3 startWorld = getStart(visual, partialTick);
        Vec3 tipWorld = getTip(visual, partialTick);
        Vector3f start = relativeTo(camera, startWorld.x, startWorld.y, startWorld.z);
        Vector3f tip = relativeTo(camera, tipWorld.x, tipWorld.y, tipWorld.z);
        float halfWidth = getHalfWidth(visual);
        float startHalfWidth = isFirstPersonChanneler(visual.getEntity()) ? halfWidth * HAND_TAPER : halfWidth;
        layeredFrustum(visual, partialTick, buffer, pose, start, tip, startHalfWidth, halfWidth, color, SPIN, FLOW);
    }
}
