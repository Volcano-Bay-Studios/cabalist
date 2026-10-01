package xyz.volcanobay.cabalist.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.client.renderer.glyph.Glyph;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphRenderer;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphStyle;
import xyz.volcanobay.cabalist.entity.LifeforceGlyph;

public class LifeforceGlyphRenderer extends EntityRenderer<LifeforceGlyph> {
    private static final float SIZE = 0.3f;
    private static final float BOB = 0.05f;
    private static final int COLOR = 0xFF55FF;

    public LifeforceGlyphRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(@NotNull LifeforceGlyph entity, float entityYaw, float partialTick, @NotNull PoseStack pose, @NotNull MultiBufferSource buffers, int packedLight) {
        float fade = entity.getFade(partialTick);
        if (fade > 0) {
            float bob = Mth.sin((entity.tickCount + partialTick) * 0.1f) * BOB;
            Vec3 at = entity.getPosition(partialTick).add(0, entity.getBbHeight() / 2 + bob, 0);
            GlyphRenderer.LOOSE.billboard(entityRenderDispatcher.camera, Glyph.rune(entity.getCharacter()), at, SIZE,
                    FastColor.ARGB32.color(Math.round(fade * 255), COLOR), entity.getId() * GlyphRenderer.SPREAD);
        }
        super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull LifeforceGlyph entity) {
        return GlyphStyle.RUNE.getSheet();
    }
}
