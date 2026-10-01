package xyz.volcanobay.cabalist.client.renderer.form;

import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3d;
import org.joml.Vector3f;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircle;
import xyz.volcanobay.cabalist.client.spell.ActiveVisual;
import xyz.volcanobay.cabalist.util.ColorHelper;

import java.util.List;

/**
 * Draws one kind of form. Layers are bound to the form, so shells and particle emitters are reshapped to fill the form.
 */
public abstract class FormRenderer {
    private static final int SHELL_COUNT = 3;
    private static final float SHELL_GROWTH = 0.35f;
    private static final float SHELL_ALPHA_FALLOFF = 0.5f;
    private static final float SHELL_SPIN_SPREAD = 0.35f;
    private static final float SHELL_FLOW_SPREAD = 0.4f;
    private static final float BREATHE_SPEED = 0.12f;
    private static final float BREATHE_AMOUNT = 0.06f;
    private static final float FLICKER_SPEED = 0.45f;
    private static final float FLICKER_AMOUNT = 0.25f;
    private static final float MIN_POP_SCALE = 0.4f;
    private static final Vector3f UP = new Vector3f(0, 1, 0);
    private static final Vector3f EAST = new Vector3f(1, 0, 0);

    public abstract double getVolume(ActiveVisual visual);

    /**
     * Finds random point in form
     */
    public abstract void samplePoint(ActiveVisual visual, RandomSource random, float partialTick, Vector3d out);

    public abstract void buildLayer(ActiveVisual visual, VertexConsumer buffer, MatrixStack pose, Camera camera, float partialTick, int color);

    public abstract void collectCircles(ActiveVisual visual, float partialTick, List<MagicCircle> out);

    /**
     * nested shells of one frustum, each larger and fainter than the last, so the form has depth.
     */
    public record ShellStyle(float startAlpha, float endAlpha, boolean capped) {
        // Shells can fade along their length, and leave their ends open so they don't cover what's under them.
        public static final ShellStyle SOLID = new ShellStyle(1, 1, true);
    }

    // those whose shape would be invisible without a layer get a plain arcane one when their aspects have none.
    public boolean needsDefaultLayer() {
        return false;
    }

    protected static void layeredFrustum(ActiveVisual visual, float partialTick, VertexConsumer buffer, MatrixStack pose, Vector3f start, Vector3f end,
                                         float startHalfWidth, float endHalfWidth, int color, float spin, float flow) {
        layeredFrustum(visual, partialTick, buffer, pose, start, end, startHalfWidth, endHalfWidth, color, spin, flow, ShellStyle.SOLID);
    }

    protected static void layeredFrustum(ActiveVisual visual, float partialTick, VertexConsumer buffer, MatrixStack pose, Vector3f start, Vector3f end,
                                         float startHalfWidth, float endHalfWidth, int color, float spin, float flow, ShellStyle style) {
        float time = visual.getTime(partialTick);
        float fade = visual.getFade(partialTick);
        float pop = MIN_POP_SCALE + (1 - MIN_POP_SCALE) * fade;
        float alpha = ColorHelper.getAlpha(color) * fade;
        for (int shell = 0; shell < SHELL_COUNT; shell++) {
            float direction = shell % 2 == 0 ? 1 : -1;
            float angle = time * spin * direction * (1 + shell * SHELL_SPIN_SPREAD);
            float breathe = 1 + shell * SHELL_GROWTH + Mth.sin(time * BREATHE_SPEED + shell * 1.9f) * BREATHE_AMOUNT;
            float flicker = 1 - FLICKER_AMOUNT * (0.5f + 0.5f * Mth.sin(time * FLICKER_SPEED + shell * 2.3f));
            float shellAlpha = alpha * flicker * (float) Math.pow(SHELL_ALPHA_FALLOFF, shell);
            float scale = breathe * pop;
            frustum(buffer, pose, start, end, startHalfWidth * scale, endHalfWidth * scale,
                    ColorHelper.withAlpha(color, shellAlpha * style.startAlpha()), ColorHelper.withAlpha(color, shellAlpha * style.endAlpha()), style.capped(),
                    angle, time * flow * (1 + shell * SHELL_FLOW_SPREAD), shell * 0.31f);
        }
    }

    protected static void frustum(VertexConsumer buffer, MatrixStack pose, Vector3f start, Vector3f end, float startHalfWidth, float endHalfWidth,
                                  int startColor, int endColor, boolean capped, float angle, float uOffset, float vOffset) {
        Vector3f forward = new Vector3f(end).sub(start);
        float length = forward.length();
        if (length < 1.0e-4f) {
            return;
        }
        forward.div(length);
        Vector3f reference = Math.abs(forward.y) < 0.99f ? UP : EAST;
        Vector3f baseRight = forward.cross(reference, new Vector3f()).normalize();
        Vector3f baseUp = baseRight.cross(forward, new Vector3f()).normalize();
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        Vector3f right = new Vector3f(baseRight).mul(cos).add(baseUp.x * sin, baseUp.y * sin, baseUp.z * sin);
        Vector3f up = new Vector3f(baseUp).mul(cos).sub(baseRight.x * sin, baseRight.y * sin, baseRight.z * sin);

        Vector3f[] startCorners = corners(start, right, up, startHalfWidth);
        Vector3f[] endCorners = corners(end, right, up, endHalfWidth);
        for (int side = 0; side < 4; side++) {
            int next = (side + 1) % 4;
            quad(buffer, pose, startCorners[side], startCorners[next], endCorners[next], endCorners[side], uOffset, vOffset, length, 1, startColor, endColor);
        }
        if (capped) {
            quad(buffer, pose, startCorners[0], startCorners[1], startCorners[2], startCorners[3], uOffset, vOffset, 1, 1, startColor, startColor);
            quad(buffer, pose, endCorners[3], endCorners[2], endCorners[1], endCorners[0], uOffset, vOffset, 1, 1, endColor, endColor);
        }
    }

    private static Vector3f[] corners(Vector3f center, Vector3f right, Vector3f up, float halfWidth) {
        return new Vector3f[]{
                new Vector3f(center).sub(right.x * halfWidth, right.y * halfWidth, right.z * halfWidth).sub(up.x * halfWidth, up.y * halfWidth, up.z * halfWidth),
                new Vector3f(center).add(right.x * halfWidth, right.y * halfWidth, right.z * halfWidth).sub(up.x * halfWidth, up.y * halfWidth, up.z * halfWidth),
                new Vector3f(center).add(right.x * halfWidth, right.y * halfWidth, right.z * halfWidth).add(up.x * halfWidth, up.y * halfWidth, up.z * halfWidth),
                new Vector3f(center).sub(right.x * halfWidth, right.y * halfWidth, right.z * halfWidth).add(up.x * halfWidth, up.y * halfWidth, up.z * halfWidth)
        };
    }

    protected static void quad(VertexConsumer buffer, MatrixStack pose, Vector3f first, Vector3f second, Vector3f third, Vector3f fourth,
                               float uOffset, float vOffset, float uMax, float vMax, int firstColor, int lastColor) {
        buffer.addVertex(pose.pose(), first.x, first.y, first.z).setUv(uOffset, vOffset).setColor(firstColor);
        buffer.addVertex(pose.pose(), second.x, second.y, second.z).setUv(uOffset, vOffset + vMax).setColor(firstColor);
        buffer.addVertex(pose.pose(), third.x, third.y, third.z).setUv(uOffset + uMax, vOffset + vMax).setColor(lastColor);
        buffer.addVertex(pose.pose(), fourth.x, fourth.y, fourth.z).setUv(uOffset + uMax, vOffset).setColor(lastColor);
    }

    protected static Vector3f relativeTo(Camera camera, double x, double y, double z) {
        return new Vector3f((float) (x - camera.getPosition().x), (float) (y - camera.getPosition().y), (float) (z - camera.getPosition().z));
    }

    protected static void randomInDisc(RandomSource random, double radius, Vector3d out) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = Math.sqrt(random.nextDouble()) * radius;
        out.set(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
    }
}
