package xyz.volcanobay.cabalist.client.renderer.item;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniformAccess;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphRenderer;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphStyle;
import xyz.volcanobay.cabalist.system.focus.Focus;
import xyz.volcanobay.cabalist.system.focus.Imbuement;

import java.util.Set;

public final class LifeforceGlint {
    private static final ResourceLocation SHADER = Cabalist.id("spell/lifeforce_glint");
    private static final int COLOR = 0xFF55FF;
    private static final double STRONGEST = 100000;
    private static final float CELLS = 3;
    private static final float ARMOR_TEXTURE_WIDTH = 64;
    private static final float ARMOR_TEXTURE_HEIGHT = 32;
    private static final float ARMOR_CELL = 4;

    private static final float SEED_SPREAD = 1000;
    private static final RenderType TYPE = create("cabalist_lifeforce_glint", RenderStateShard.NO_LAYERING);
    private static final RenderType ARMOR_TYPE = create("cabalist_armor_lifeforce_glint", RenderStateShard.VIEW_OFFSET_Z_LAYERING);
    private static final Set<RenderType> GLINTS = Set.of(TYPE, ARMOR_TYPE, RenderType.glint(), RenderType.glintTranslucent(), RenderType.entityGlint(),
            RenderType.entityGlintDirect(), RenderType.armorEntityGlint());

    private static RenderType create(String name, RenderStateShard.LayeringStateShard layering) {
        return RenderType.create(name, DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536,
                RenderType.CompositeState.builder()
                        .setShaderState(VeilRenderBridge.shaderState(SHADER))
                        .setTextureState(new RenderStateShard.TextureStateShard(GlyphStyle.RUNE.getSheet(), false, false))
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                        .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                        .setLayeringState(layering)
                        .setTexturingState(new RenderStateShard.TexturingStateShard(name + "_time", LifeforceGlint::setTime, () -> {
                        }))
                        .createCompositeState(false));
    }

    public static void register(RegisterRenderBuffersEvent event) {
        event.registerRenderBuffer(TYPE);
        event.registerRenderBuffer(ARMOR_TYPE);
    }

    public static @Nullable VertexConsumer getBuffer(ItemStack stack, MultiBufferSource buffers, PoseStack.Pose pose) {
        Style style = getStyle(stack);
        return style == null ? null : new SurfaceUvs(buffers.getBuffer(TYPE), pose, style);
    }

    public static MultiBufferSource wrap(ItemStack stack, MultiBufferSource buffers, PoseStack.Pose pose) {
        Style style = getStyle(stack);
        if (style == null) {
            return buffers;
        }
        PoseStack.Pose snapshot = pose.copy();
        return type -> GLINTS.contains(type) ? buffers.getBuffer(type)
                : VertexMultiConsumer.create(new SurfaceUvs(buffers.getBuffer(TYPE), snapshot, style), buffers.getBuffer(type));
    }

    public static @Nullable VertexConsumer getArmorBuffer(ItemStack stack, MultiBufferSource buffers) {
        Style style = getStyle(stack);
        return style == null ? null : new ArmorUvs(buffers.getBuffer(ARMOR_TYPE), style);
    }

    private static @Nullable Style getStyle(ItemStack stack) {
        if (!Focus.canHold(stack)) {
            return null;
        }
        Imbuement imbuement = Focus.get(stack);
        double lifeforce = imbuement.getTotalLifeforce();
        if (lifeforce <= 0) {
            return null;
        }
        float strength = (float) Math.min(1, Math.log1p(lifeforce) / Math.log1p(STRONGEST));
        int seed = imbuement.seed() != 0 ? imbuement.seed() : BuiltInRegistries.ITEM.getKey(stack.getItem()).hashCode();
        RandomSource random = RandomSource.create(seed);
        return new Style(FastColor.ARGB32.color(Math.max(1, Math.round(strength * 255)), COLOR),
                (float) Math.floor(random.nextFloat() * SEED_SPREAD), (float) Math.floor(random.nextFloat() * SEED_SPREAD));
    }

    private record Style(int color, float offsetU, float offsetV) {
    }

    private static void setTime() {
        ShaderProgram shader = VeilRenderSystem.renderer().getShaderManager().getShader(SHADER);
        ShaderUniformAccess time = shader == null ? null : shader.getUniform("GlintTime");
        if (time != null) {
            time.setFloat(GlyphRenderer.getTime());
        }
    }

    private abstract static class GlintUvs implements VertexConsumer {
        private final VertexConsumer delegate;
        private final Style style;

        private GlintUvs(VertexConsumer delegate, Style style) {
            this.delegate = delegate;
            this.style = style;
        }

        protected void write(float x, float y, float z, float u, float v) {
            delegate.addVertex(x, y, z).setUv(u + style.offsetU(), v + style.offsetV()).setColor(style.color());
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            emit(x, y, z, 0, 0, new Vector3f(0, 0, 1));
            return this;
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int packedOverlay, int packedLight, float normalX, float normalY, float normalZ) {
            emit(x, y, z, u, v, new Vector3f(normalX, normalY, normalZ));
        }

        protected abstract void emit(float x, float y, float z, float u, float v, Vector3f normal);

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            return this;
        }
    }

    private static final class SurfaceUvs extends GlintUvs {
        private final Matrix4f toModel;
        private final Matrix3f toModelNormal;

        private SurfaceUvs(VertexConsumer delegate, PoseStack.Pose pose, Style style) {
            super(delegate, style);
            this.toModel = new Matrix4f(pose.pose()).invert();
            this.toModelNormal = new Matrix3f(pose.normal()).transpose();
        }

        @Override
        protected void emit(float x, float y, float z, float textureU, float textureV, Vector3f normal) {
            Vector3f local = toModel.transformPosition(x, y, z, new Vector3f());
            toModelNormal.transform(normal);
            float nx = Math.abs(normal.x);
            float ny = Math.abs(normal.y);
            float nz = Math.abs(normal.z);
            float u;
            float v;
            if (nx >= ny && nx >= nz) {
                u = local.z;
                v = local.y;
            } else if (ny >= nz) {
                u = local.x;
                v = local.z;
            } else {
                u = local.x;
                v = local.y;
            }
            write(x, y, z, u * CELLS, v * CELLS);
        }
    }

    private static final class ArmorUvs extends GlintUvs {
        private ArmorUvs(VertexConsumer delegate, Style style) {
            super(delegate, style);
        }

        @Override
        protected void emit(float x, float y, float z, float u, float v, Vector3f normal) {
            write(x, y, z, u * ARMOR_TEXTURE_WIDTH / ARMOR_CELL, -v * ARMOR_TEXTURE_HEIGHT / ARMOR_CELL);
        }
    }
}
