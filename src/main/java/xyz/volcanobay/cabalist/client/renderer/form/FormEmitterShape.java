package xyz.volcanobay.cabalist.client.renderer.form;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.quasar.emitters.shape.EmitterShape;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;
import xyz.volcanobay.cabalist.client.spell.ActiveVisual;

/**
 * Lets a Quasar emitter spawn inside a form's shape instead of the shape it was defined with.
 */
public class FormEmitterShape implements EmitterShape {
    private final FormRenderer renderer;
    private final ActiveVisual visual;

    public FormEmitterShape(FormRenderer renderer, ActiveVisual visual) {
        this.renderer = renderer;
        this.visual = visual;
    }

    @Override
    public @NotNull Vector3d getPoint(@NotNull RandomSource random, @NotNull Vector3fc dimensions, @NotNull Vector3fc rotation,
                                      @NotNull Vector3dc position, boolean fromSurface) {
        Vector3d point = new Vector3d();
        renderer.samplePoint(visual, random, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), point);
        return point;
    }

    @Override
    public void renderShape(@NotNull PoseStack poseStack, @NotNull VertexConsumer consumer, @NotNull Vector3fc dimensions, @NotNull Vector3fc rotation) {
    }
}
