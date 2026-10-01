package xyz.volcanobay.cabalist.client.renderer.circle;

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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.client.casting.ClientCastingSystem;
import xyz.volcanobay.cabalist.client.renderer.SpellRenderStates;
import xyz.volcanobay.cabalist.client.renderer.form.FormRenderers;
import xyz.volcanobay.cabalist.client.renderer.glyph.Glyph;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphRenderer;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphStyle;
import xyz.volcanobay.cabalist.client.renderer.glyph.Glyphs;
import xyz.volcanobay.cabalist.client.request.DraftCircles;
import xyz.volcanobay.cabalist.client.request.NotificationCircles;
import xyz.volcanobay.cabalist.client.spell.ActiveVisual;
import xyz.volcanobay.cabalist.client.spell.ClientHangingSpellSystem;
import xyz.volcanobay.cabalist.client.visibility.ClientHidingSystem;
import xyz.volcanobay.cabalist.system.render.Palette;
import xyz.volcanobay.cabalist.util.ColorHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MagicCircleRenderer {
    private static final ResourceLocation CIRCLE_SHADER = Cabalist.id("spell/magic_circle");
    private static final float TEXELS_PER_BLOCK = 16;
    private static final int MIN_RADIUS_TEXELS = 4;
    private static final int MAX_RADIUS_TEXELS = Short.MAX_VALUE;
    private static final int DRAW_IN_SCALE = Short.MAX_VALUE;
    private static final float RUNE_SPEED = 0.32f;
    private static final float AWAITING_TEXT_PIXEL = 0.025f;
    private static final float AWAITING_TEXT_HEIGHT = 0.6f;
    private static final float AWAITING_TEXT_ALPHA = 0.85f;
    private static final float AWAITING_BOB_SPEED = 0.12f;
    private static final float AWAITING_BOB_SPREAD = 0.7f;
    private static final float AWAITING_BOB_HEIGHT = 0.02f;
    private static final float THROUGH_BLOCKS_ALPHA = 0.3f;
    private static final Map<CircleType, RenderType> TYPES = new HashMap<>();

    private final List<MagicCircle> collected = new ArrayList<>();
    private final List<CircleDraw> draws = new ArrayList<>();
    private final List<Placed> placed = new ArrayList<>();
    private int[] offsets = new int[32];

    public record CircleDraw(MagicCircle circle, int seed, float time, float fade, float brightness, Palette palette, String words, float charge, String awaiting,
                             boolean isAwaitingLatin, boolean isThroughBlocks) {
        public CircleDraw(MagicCircle circle, int seed, float time, float fade, float brightness, Palette palette, String words, float charge, String awaiting,
                          boolean isAwaitingLatin) {
            this(circle, seed, time, fade, brightness, palette, words, charge, awaiting, isAwaitingLatin, false);
        }

        public CircleDraw(MagicCircle circle, int seed, float time, float fade, float brightness, Palette palette, String words, float charge, String awaiting) {
            this(circle, seed, time, fade, brightness, palette, words, charge, awaiting, false, false);
        }

        public CircleDraw faded(float alpha) {
            return new CircleDraw(circle, seed, time, fade * alpha, brightness, palette, words, charge, awaiting, isAwaitingLatin, isThroughBlocks);
        }

        public CircleDraw throughBlocks() {
            return new CircleDraw(circle, seed, time, fade, brightness, palette, words, charge, awaiting, isAwaitingLatin, true);
        }
    }

    public void renderCircles(List<ActiveVisual> visuals, MatrixStack pose, MultiBufferSource.BufferSource buffers, Camera camera, float partialTick, boolean bloom) {
        draws.clear();
        for (ActiveVisual visual : visuals) {
            if (!visual.isPlaced() || visual.isSuppressed()) {
                continue;
            }
            float alpha = ClientHidingSystem.INSTANCE.getVisual(visual.getId());
            collected.clear();
            FormRenderers.get(visual.getShape().kind()).collectCircles(visual, partialTick, collected);
            for (int i = 0; i < collected.size(); i++) {
                float time = visual.getTime(partialTick);
                draws.add(new CircleDraw(collected.get(i), (visual.getId() * 31 + i) & MagicCircle.SEED_MASK, visual.getPhase(partialTick),
                        visual.getFade(partialTick) * alpha, CircleEnergy.getBrightness(visual.getFlow(), visual.getRemaining(), time),
                        visual.getPalette(), visual.getWords(), 1, ""));
            }
        }
        ClientHangingSpellSystem.INSTANCE.collectCircles(partialTick, draws);
        ClientCastingSystem.INSTANCE.collectCircles(camera, partialTick, draws);
        NotificationCircles.INSTANCE.collectCircles(partialTick, draws);
        DraftCircles.INSTANCE.collectCircles(partialTick, draws);

        placed.clear();
        for (CircleDraw draw : draws) {
            if (draw.fade() > 0) {
                placed.add(new Placed(draw, camera.getPosition()));
            }
        }
        if (placed.isEmpty()) {
            return;
        }
        drawCircles(pose, buffers, bloom);
        GlyphRenderer glyphs = GlyphRenderer.INSTANCE.begin(GlyphRenderer.Depth.TESTED, bloom);
        Vec3 textRight = new Vec3(camera.getLeftVector()).scale(-1);
        Vec3 textUp = new Vec3(camera.getUpVector());
        for (Placed circle : placed) {
            buildRunes(glyphs, pose, circle);
            if (!circle.draw.awaiting().isEmpty()) {
                buildAwaitingText(glyphs, pose, circle, textRight, textUp);
            }
        }
        glyphs.end();
    }

    private void drawCircles(MatrixStack pose, MultiBufferSource.BufferSource buffers, boolean bloom) {
        ShaderProgram shader = VeilRenderSystem.renderer().getShaderManager().getShader(CIRCLE_SHADER);
        if (shader == null) {
            return;
        }
        ShaderUniformAccess time = shader.getUniform("CircleTime");
        for (Placed circle : placed) {
            drawCircle(pose, buffers, time, circle, 1, new CircleType(circle.draw.palette().hasVoid(), false, bloom));
        }
        // Only where something in front hides it.
        for (Placed circle : placed) {
            if (circle.draw.isThroughBlocks()) {
                drawCircle(pose, buffers, time, circle, THROUGH_BLOCKS_ALPHA, new CircleType(circle.draw.palette().hasVoid(), true, bloom));
            }
        }
    }

    private static void drawCircle(MatrixStack pose, MultiBufferSource.BufferSource buffers, @Nullable ShaderUniformAccess time, Placed circle, float alpha, CircleType type) {
        RenderType renderType = TYPES.computeIfAbsent(type, MagicCircleRenderer::createType);
        VertexConsumer buffer = buffers.getBuffer(renderType);
        float half = (circle.radiusTexels + 1) / TEXELS_PER_BLOCK;
        float opacity = circle.draw.fade() * circle.draw.brightness() * alpha;
        int points = circle.layout.starPoints() - CircleLayout.MIN_STAR_POINTS;
        int data = circle.radiusTexels | (circle.draw.seed() | circle.layout.rings().size() << 9 | points << 12) << 16;
        int drawIn = Math.round(Mth.clamp(circle.drawIn, 0, 1) * DRAW_IN_SCALE);
        corner(buffer, pose, circle, -half, -half, opacity, data, drawIn);
        corner(buffer, pose, circle, half, -half, opacity, data, drawIn);
        corner(buffer, pose, circle, half, half, opacity, data, drawIn);
        corner(buffer, pose, circle, -half, half, opacity, data, drawIn);
        if (time != null) {
            time.setFloat(circle.draw.time());
        }
        buffers.endBatch(renderType);
    }

    private record CircleType(boolean isVoid, boolean isThroughBlocks, boolean bloom) {
    }

    private static RenderType createType(CircleType type) {
        return RenderType.create("cabalist_magic_circle" + (type.isVoid() ? "_void" : "") + (type.isThroughBlocks() ? "_through_blocks" : "") + (type.bloom() ? "_bloom" : ""),DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, RenderType.TRANSIENT_BUFFER_SIZE, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(VeilRenderBridge.shaderState(CIRCLE_SHADER))
                        .setTransparencyState(type.isVoid() ? SpellRenderStates.DEFAULT_TRANSPARENCY : RenderStateShard.LIGHTNING_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(type.isThroughBlocks() ? RenderStateShard.GREATER_DEPTH_TEST : RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setOutputState(SpellRenderStates.output(type.bloom()))
                        .createCompositeState(false));
    }

    private static void corner(VertexConsumer buffer, MatrixStack pose, Placed circle, float x, float y, float opacity, int data, int drawIn) {
        int color = ColorHelper.withAlpha(circle.draw.palette().sample((float) Mth.atan2(y, x) / Mth.TWO_PI, GlyphRenderer.getTime()), opacity);
        Vec3 at = circle.center.add(circle.right.scale(x)).add(circle.up.scale(y));
        buffer.addVertex(pose.pose(), (float) at.x, (float) at.y, (float) at.z).setColor(color).setUv(x, y).setOverlay(drawIn).setLight(data)
                .setNormal((float) circle.normal.x, (float) circle.normal.y, (float) circle.normal.z);
    }

    public static @Nullable RuneSlot getRuneSlot(MagicCircle circle, String words, int letter, float time) {
        Vec3 right = circle.getRight();
        Vec3 up = circle.getUp();
        int radiusTexels = getRadiusTexels(circle);
        List<List<Glyph>> rings = CircleLayout.of(words, radiusTexels).rings();
        int index = letter;
        for (int ring = 0; ring < rings.size() && index >= 0; ring++) {
            int count = rings.get(ring).size();
            if (index < count) {
                float ringRadius = CircleLayout.getRingCenter(radiusTexels, ring);
                float speed = ring % 2 == 0 ? RUNE_SPEED : -RUNE_SPEED;
                float angle = time * speed / ringRadius - Mth.TWO_PI * index / count;
                Vec3 outward = right.scale(Mth.cos(angle)).add(up.scale(Mth.sin(angle)));
                Vec3 along = up.scale(Mth.cos(angle)).subtract(right.scale(Mth.sin(angle)));
                return new RuneSlot(outward.scale(ringRadius / TEXELS_PER_BLOCK), along.scale(-1), outward);
            }
            index -= count;
        }
        return null;
    }

    public record RuneSlot(Vec3 offset, Vec3 right, Vec3 up) {
    }

    public static int getRadiusTexels(MagicCircle circle) {
        return Mth.clamp(Math.round(circle.radius() * TEXELS_PER_BLOCK), MIN_RADIUS_TEXELS, MAX_RADIUS_TEXELS);
    }

    public static float getRuneCell() {
        return Glyphs.CELL / TEXELS_PER_BLOCK;
    }

    private void buildRunes(GlyphRenderer glyphs, MatrixStack pose, Placed circle) {
        List<List<Glyph>> rings = circle.layout.rings();
        for (int ring = 0; ring < rings.size(); ring++) {
            float speed = ring % 2 == 0 ? RUNE_SPEED : -RUNE_SPEED;
            placeRuneRing(glyphs, pose, circle, rings.get(ring), CircleLayout.getRingCenter(circle.radiusTexels, ring), speed);
        }
    }

    // floating above a waiting spell's circle
    private void buildAwaitingText(GlyphRenderer glyphs, MatrixStack pose, Placed circle, Vec3 right, Vec3 up) {
        List<Glyph> text = Glyph.of(circle.draw.awaiting().toLowerCase(), circle.draw.isAwaitingLatin() ? GlyphStyle.LATIN : GlyphStyle.RUNE);
        if (text.size() > offsets.length) {
            offsets = new int[text.size()];
        }
        int width = GlyphRenderer.layout(text, offsets);
        Vec3 above = circle.center.add(circle.normal.scale(AWAITING_TEXT_HEIGHT));
        float cellSize = Glyphs.CELL * AWAITING_TEXT_PIXEL;
        float opacity = circle.draw.fade() * AWAITING_TEXT_ALPHA;
        float time = GlyphRenderer.getTime();
        for (int i = 0; i < text.size(); i++) {
            float x = (offsets[i] - width / 2f) * AWAITING_TEXT_PIXEL + cellSize / 2;
            float bob = Mth.sin(circle.draw.time() * AWAITING_BOB_SPEED + i * AWAITING_BOB_SPREAD) * AWAITING_BOB_HEIGHT;
            int color = ColorHelper.withAlpha(circle.draw.palette().sample((float) i / text.size(), time), opacity);
            glyphs.glyph(pose, text.get(i), above.add(right.scale(x)).add(up.scale(bob)), right, up, cellSize, color, i * GlyphRenderer.SPREAD);
        }
    }

    private void placeRuneRing(GlyphRenderer glyphs, MatrixStack pose, Placed circle, List<Glyph> text, float radiusTexels, float speed) {
        int count = text.size();
        float turn = circle.draw.time() * speed / radiusTexels;
        float radius = radiusTexels / TEXELS_PER_BLOCK;
        for (int i = 0; i < count; i++) {
            if (i >= count * circle.drawIn) {
                break;
            }
            // Written clockwise so the words read in order around the ring.
            float angle = turn - Mth.TWO_PI * i / count;
            Vec3 outward = circle.right.scale(Mth.cos(angle)).add(circle.up.scale(Mth.sin(angle)));
            Vec3 along = circle.up.scale(Mth.cos(angle)).subtract(circle.right.scale(Mth.sin(angle)));
            glyphs.glyph(pose, text.get(i), circle.center.add(outward.scale(radius)), along.scale(-1), outward, Glyphs.CELL / TEXELS_PER_BLOCK,
                    ColorHelper.withAlpha(circle.draw.palette().sample(angle / Mth.TWO_PI, GlyphRenderer.getTime()), circle.draw.fade() * circle.draw.brightness()), angle * 3);
        }
    }

    private static final class Placed {
        private final CircleDraw draw;
        private final Vec3 center;
        private final Vec3 right;
        private final Vec3 up;
        private final Vec3 normal;
        private final int radiusTexels;
        private final float drawIn;
        private final CircleLayout layout;

        private Placed(CircleDraw draw, Vec3 camera) {
            this.draw = draw;
            this.center = draw.circle().center().subtract(camera);
            this.normal = draw.circle().normal().normalize();
            this.right = draw.circle().getRight();
            this.up = draw.circle().getUp();
            this.radiusTexels = getRadiusTexels(draw.circle());
            this.drawIn = Math.min(1 - (1 - draw.fade()) * (1 - draw.fade()) * (1 - draw.fade()), draw.charge());
            this.layout = CircleLayout.of(draw.words(), radiusTexels);
        }
    }
}
