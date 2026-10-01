package xyz.volcanobay.cabalist.client.renderer.glyph;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.client.render.VeilRenderBridge;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import xyz.volcanobay.cabalist.client.renderer.SpellRenderStates;
import xyz.volcanobay.cabalist.system.render.Palette;
import xyz.volcanobay.cabalist.util.ColorHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

/**
 * Draws glyphs!
 */
public final class GlyphRenderer {
    public static final float SPREAD = 0.7f;
    private static final float WAVE_SPEED = 0.15f;
    private static final float WAVE_LOW = 0.55f;
    private static final float GRADIENT = 0.6f;
    private static final float MILLIS_PER_TICK = 50;
    private static final long CLOCK_WRAP = 1L << 22;
    private static final MatrixStack IDENTITY = VeilRenderBridge.create(new PoseStack());
    private static final Map<Batch, RenderType> TYPES = new LinkedHashMap<>();

    static {
        for (Depth depth : Depth.values()) {
            for (GlyphStyle style : GlyphStyle.values()) {
                for (boolean dark : new boolean[]{false, true}) {
                    for (boolean bloom : new boolean[]{false, true}) {
                        Batch batch = new Batch(style, dark, depth, bloom);
                        TYPES.put(batch, createType(batch));
                    }
                }
            }
        }
    }

    public static final GlyphRenderer INSTANCE = new GlyphRenderer();
    public static final GlyphRenderer LOOSE = new GlyphRenderer();

    private final MultiBufferSource.BufferSource buffers;
    private Depth depth = Depth.TESTED;
    private boolean bloom;

    public enum Depth {
        TESTED(RenderStateShard.LEQUAL_DEPTH_TEST),
        OCCLUDED(RenderStateShard.GREATER_DEPTH_TEST),
        ALWAYS(RenderStateShard.NO_DEPTH_TEST);

        private final RenderStateShard.DepthTestStateShard shard;

        Depth(RenderStateShard.DepthTestStateShard shard) {
            this.shard = shard;
        }
    }

    private GlyphRenderer() {
        SequencedMap<RenderType, ByteBufferBuilder> fixed = new LinkedHashMap<>();
        for (RenderType type : TYPES.values()) {
            fixed.put(type, new ByteBufferBuilder(type.bufferSize()));
        }
        this.buffers = MultiBufferSource.immediateWithBuffers(fixed, new ByteBufferBuilder(RenderType.TRANSIENT_BUFFER_SIZE));
    }

    private static RenderType createType(Batch batch) {
        String name = "cabalist_glyph_" + batch.style().getName() + (batch.dark() ? "_dark_" : "_") + batch.depth().name().toLowerCase() + (batch.bloom() ? "_bloom" : "");
        return RenderType.create(name, DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, RenderType.TRANSIENT_BUFFER_SIZE, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(batch.style().getSheet(), false, false))
                        .setTransparencyState(batch.dark() ? SpellRenderStates.DEFAULT_TRANSPARENCY : RenderStateShard.LIGHTNING_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(batch.depth().shard)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setOutputState(SpellRenderStates.output(batch.bloom()))
                        .createCompositeState(false));
    }

    public GlyphRenderer begin(Depth depth) {
        return begin(depth, false);
    }

    public GlyphRenderer begin(Depth depth, boolean bloom) {
        this.depth = depth;
        this.bloom = bloom;
        return this;
    }

    /**
     * A glyph centered on a point, lying in the plane of {@code right} and {@code up}, which should be unit length.
     */
    public void glyph(MatrixStack pose, Glyph glyph, Vec3 center, Vec3 right, Vec3 up, float cellSize, int color, float phase) {
        if (!Glyphs.isDrawable(glyph)) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(TYPES.get(new Batch(glyph.style(), Palette.isVoid(color), depth, bloom)));
        center = center.add(right.scale(Glyphs.getCenterShift(glyph) * cellSize));
        Vec3 halfRight = right.scale(cellSize / 2);
        Vec3 halfUp = up.scale(cellSize / 2);
        float u0 = Glyphs.u0(glyph);
        float v0 = Glyphs.v0(glyph);
        float u1 = u0 + Glyphs.cellSize();
        float v1 = v0 + Glyphs.cellSize();
        float time = getTime();
        int left = pulse(color, phase - GRADIENT / 2, time);
        int rightColor = pulse(color, phase + GRADIENT / 2, time);
        vertex(buffer, pose, center.subtract(halfRight).subtract(halfUp), u0, v1, left);
        vertex(buffer, pose, center.add(halfRight).subtract(halfUp), u1, v1, rightColor);
        vertex(buffer, pose, center.add(halfRight).add(halfUp), u1, v0, rightColor);
        vertex(buffer, pose, center.subtract(halfRight).add(halfUp), u0, v0, left);
    }

    public void billboard(Camera camera, Glyph glyph, Vec3 worldCenter, float cellSize, int color, float phase) {
        Vec3 right = new Vec3(camera.getLeftVector()).scale(-1);
        Vec3 up = new Vec3(camera.getUpVector());
        glyph(IDENTITY, glyph, worldCenter.subtract(camera.getPosition()), right, up, cellSize, color, phase);
    }

    public void end() {
        buffers.endBatch();
    }

    public static float getTime() {
        return (Util.getMillis() % (CLOCK_WRAP * (long) MILLIS_PER_TICK)) / MILLIS_PER_TICK;
    }

    private static int pulse(int color, float phase, float time) {
        float wave = WAVE_LOW + (1 - WAVE_LOW) * (0.5f + 0.5f * Mth.sin(phase - time * WAVE_SPEED));
        return ColorHelper.withAlpha(color, ColorHelper.getAlpha(color) * wave);
    }

    public static int layout(List<Glyph> glyphs, int[] offsets) {
        int x = 0;
        for (int i = 0; i < glyphs.size(); i++) {
            offsets[i] = x;
            x += Glyphs.advance(glyphs.get(i));
        }
        return x;
    }

    private record Batch(GlyphStyle style, boolean dark, Depth depth, boolean bloom) {
    }

    private static void vertex(VertexConsumer buffer, MatrixStack pose, Vec3 at, float u, float v, int color) {
        buffer.addVertex(pose.pose(), (float) at.x, (float) at.y, (float) at.z).setUv(u, v).setColor(color);
    }
}
