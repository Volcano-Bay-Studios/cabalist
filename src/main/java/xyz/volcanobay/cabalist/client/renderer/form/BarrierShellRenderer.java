package xyz.volcanobay.cabalist.client.renderer.form;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniformAccess;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.client.renderer.SpellRenderStates;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphRenderer;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphStyle;
import xyz.volcanobay.cabalist.client.spell.ActiveVisual;
import xyz.volcanobay.cabalist.client.visibility.ClientHidingSystem;
import xyz.volcanobay.cabalist.system.barrier.Barrier;
import xyz.volcanobay.cabalist.system.render.FormShape;
import xyz.volcanobay.cabalist.system.render.Palette;
import xyz.volcanobay.cabalist.system.render.RenderSpec;
import xyz.volcanobay.cabalist.util.ColorHelper;

public final class BarrierShellRenderer {
    private static final ResourceLocation SHADER = Cabalist.id("spell/barrier");
    private static final ResourceLocation VOID_SHADER = Cabalist.id("spell/void");
    private static final float SHELL_ALPHA = 0.6f;
    private static final int MIN_SEGMENTS = 24;
    private static final int MAX_SEGMENTS = 96;
    private static final int SEGMENTS_PER_BLOCK = 6;
    private static final int CAP_RINGS = 8;
    private static final int MIN_CELLS_AROUND = 6;
    private static final double GLYPH_CELL = 0.45;
    private static final float OPAQUE = 0.99f;
    private static final RenderType SHELL_TYPE = RenderType.create("cabalist_barrier_shell", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS,
            RenderType.TRANSIENT_BUFFER_SIZE, false, false, RenderType.CompositeState.builder()
                    .setShaderState(VeilRenderBridge.shaderState(SHADER))
                    .setTextureState(new RenderStateShard.TextureStateShard(GlyphStyle.RUNE.getSheet(), false, false))
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));
    private static final RenderType VOID_TYPE = createVoidType("cabalist_void", RenderStateShard.COLOR_WRITE);
    private static final RenderType OPAQUE_VOID_TYPE = createVoidType("cabalist_opaque_void", RenderStateShard.COLOR_DEPTH_WRITE);

    private static RenderType createVoidType(String name, RenderStateShard.WriteMaskStateShard writeMask) {
        return RenderType.create(name, DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, RenderType.TRANSIENT_BUFFER_SIZE, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(VeilRenderBridge.shaderState(VOID_SHADER))
                        .setTransparencyState(SpellRenderStates.DEFAULT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(writeMask)
                        .createCompositeState(false));
    }

    public static boolean isBarrier(ActiveVisual visual) {
        for (RenderSpec spec : visual.getSpecs()) {
            if (spec.barrier()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isVoid(ActiveVisual visual) {
        return visual.hasSpokenColors() && visual.getPalette().hasVoid();
    }

    public static void render(ActiveVisual visual, MatrixStack pose, MultiBufferSource.BufferSource buffers, Camera camera, float partialTick) {
        float opacity = visual.getFade(partialTick) * ClientHidingSystem.INSTANCE.getVisual(visual.getId());
        if (opacity <= 0) {
            return;
        }
        boolean isVoid = isVoid(visual);
        ShaderProgram shader = getShader(isVoid ? VOID_SHADER : SHADER);
        if (shader == null) {
            return;
        }
        RenderType type = isVoid ? getVoidType(opacity) : SHELL_TYPE;
        Shell shell = new Shell(buffers.getBuffer(type), pose, camera.getPosition(), visual.getPalette(), GlyphRenderer.getTime(), isVoid ? 1 : opacity * SHELL_ALPHA, new Surface[1]);
        FormShape shape = visual.getShape();
        double radius = Math.max(Barrier.MIN_RADIUS, shape.radius());
        switch (shape.kind()) {
            case AREA -> shell.cylinder(shape.start(), radius, shape.height());
            case BEAM -> shell.capsule(BeamRenderer.getStart(visual, partialTick), BeamRenderer.getEnd(visual, partialTick), radius);
            case SKY_STRIKE -> {
                Vec3 bottom = SkyStrikeRenderer.getBottom(visual, partialTick);
                shell.capsule(bottom, new Vec3(bottom.x, SkyStrikeRenderer.getTop(visual, bottom), bottom.z), radius);
            }
            case POINT, PROJECTILE -> shell.sphere(getCenter(visual, partialTick), radius);
        }
        if (isVoid) {
            setVoidUniforms(shader, opacity);
        } else if (shell.surface[0] != null) {
            setShellUniforms(shader, shell.surface[0]);
        }
        buffers.endBatch(type);
    }

    public static void renderVoidForm(FormRenderer renderer, ActiveVisual visual, MatrixStack pose, MultiBufferSource.BufferSource buffers, Camera camera, float partialTick) {
        float opacity = visual.getFade(partialTick) * ClientHidingSystem.INSTANCE.getVisual(visual.getId());
        ShaderProgram shader = getShader(VOID_SHADER);
        if (opacity <= 0 || shader == null) {
            return;
        }
        RenderType type = getVoidType(opacity);
        renderer.buildLayer(visual, buffers.getBuffer(type), pose, camera, partialTick, 0xFF000000);
        setVoidUniforms(shader, opacity);
        buffers.endBatch(type);
    }

    private static Vec3 getCenter(ActiveVisual visual, float partialTick) {
        Entity followed = visual.getEntity();
        return followed == null ? visual.getStart(partialTick) : followed.getPosition(partialTick).add(0, followed.getBbHeight() / 2, 0);
    }

    private static @Nullable ShaderProgram getShader(ResourceLocation id) {
        return VeilRenderSystem.renderer().getShaderManager().getShader(id);
    }

    private static RenderType getVoidType(float opacity) {
        return opacity >= OPAQUE ? OPAQUE_VOID_TYPE : VOID_TYPE;
    }

    private static void setShellUniforms(ShaderProgram shader, Surface surface) {
        ShaderUniformAccess time = shader.getUniform("BarrierTime");
        if (time != null) {
            time.setFloat(GlyphRenderer.getTime());
        }
        ShaderUniformAccess cells = shader.getUniform("ShellCells");
        if (cells != null) {
            cells.setVector((float) surface.cellsAround(), (float) (surface.span() / GLYPH_CELL));
        }
    }

    private static void setVoidUniforms(ShaderProgram shader, float opacity) {
        ShaderUniformAccess uniform = shader.getUniform("VoidOpacity");
        if (uniform != null) {
            uniform.setFloat(Math.min(1, opacity));
        }
    }

    private static int getSegments(double radius) {
        return Mth.clamp((int) Math.ceil(radius * Mth.TWO_PI * SEGMENTS_PER_BLOCK / 4), MIN_SEGMENTS, MAX_SEGMENTS);
    }

    private record Shell(VertexConsumer buffer, MatrixStack pose, Vec3 camera, Palette palette, float time, float alpha, Surface[] surface) {
        private void cylinder(Vec3 base, double radius, double height) {
            int segments = getSegments(radius);
            Surface surface = new Surface(base, new Vec3(0, 1, 0), height, getCellsAround(radius));
            this.surface[0] = surface;
            for (int i = 0; i < segments; i++) {
                float from = (float) i / segments;
                float to = (float) (i + 1) / segments;
                Vec3 a = base.add(ring(from, radius));
                Vec3 b = base.add(ring(to, radius));
                vertex(surface, a, from);
                vertex(surface, a.add(0, height, 0), from);
                vertex(surface, b.add(0, height, 0), to);
                vertex(surface, b, to);
            }
        }

        private void sphere(Vec3 center, double radius) {
            capsule(center, center, radius);
        }

        private void capsule(Vec3 start, Vec3 end, double radius) {
            Vec3 axis = end.subtract(start);
            double length = axis.length();
            Vec3 forward = length < 1e-6 ? new Vec3(0, 1, 0) : axis.scale(1 / length);
            Vec3 reference = Math.abs(forward.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
            Vec3 right = forward.cross(reference).normalize();
            Vec3 up = right.cross(forward).normalize();
            Surface surface = new Surface(start.subtract(forward.scale(radius)), forward, length + 2 * radius, getCellsAround(radius));
            this.surface[0] = surface;
            int segments = getSegments(radius);
            for (int ring = -CAP_RINGS; ring < CAP_RINGS + 1; ring++) {
                for (int i = 0; i < segments; i++) {
                    float from = (float) i / segments;
                    float to = (float) (i + 1) / segments;
                    if (ring == 0) {
                        Vec3 a = around(right, up, from, radius);
                        Vec3 b = around(right, up, to, radius);
                        vertex(surface, start.add(a), from);
                        vertex(surface, end.add(a), from);
                        vertex(surface, end.add(b), to);
                        vertex(surface, start.add(b), to);
                    } else if (ring < 0) {
                        capQuad(surface, start, forward.scale(-1), right, up, radius, -ring - 1, -ring, from, to);
                    } else {
                        capQuad(surface, end, forward, right, up, radius, ring - 1, ring, from, to);
                    }
                }
            }
        }

        private void capQuad(Surface surface, Vec3 center, Vec3 outward, Vec3 right, Vec3 up, double radius, int inner, int outer, float from, float to) {
            double innerAngle = inner * Math.PI / 2 / CAP_RINGS;
            double outerAngle = outer * Math.PI / 2 / CAP_RINGS;
            vertex(surface, cap(center, outward, right, up, radius, innerAngle, from), from);
            vertex(surface, cap(center, outward, right, up, radius, outerAngle, from), from);
            vertex(surface, cap(center, outward, right, up, radius, outerAngle, to), to);
            vertex(surface, cap(center, outward, right, up, radius, innerAngle, to), to);
        }

        private static Vec3 cap(Vec3 center, Vec3 outward, Vec3 right, Vec3 up, double radius, double angle, float around) {
            return center.add(around(right, up, around, radius * Math.cos(angle))).add(outward.scale(radius * Math.sin(angle)));
        }

        private static Vec3 around(Vec3 right, Vec3 up, float turn, double radius) {
            double angle = turn * Mth.TWO_PI;
            return right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
        }

        private static Vec3 ring(float turn, double radius) {
            double angle = turn * Mth.TWO_PI;
            return new Vec3(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
        }

        private static double getCellsAround(double radius) {
            return Math.max(MIN_CELLS_AROUND, Math.round(radius * Mth.TWO_PI / GLYPH_CELL));
        }

        private void vertex(Surface surface, Vec3 at, float turn) {
            int color = palette.sample(turn, time);
            double v = at.subtract(surface.bottom()).dot(surface.upward()) / GLYPH_CELL;
            buffer.addVertex(pose.pose(), (float) (at.x - camera.x), (float) (at.y - camera.y), (float) (at.z - camera.z))
                    .setUv((float) (turn * surface.cellsAround()), (float) v).setColor(ColorHelper.withAlpha(color, alpha));
        }
    }

    private record Surface(Vec3 bottom, Vec3 upward, double span, double cellsAround) {
    }
}
