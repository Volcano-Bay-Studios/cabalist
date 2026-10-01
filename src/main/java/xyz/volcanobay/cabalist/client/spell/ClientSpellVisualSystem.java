package xyz.volcanobay.cabalist.client.spell;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.quasar.data.EmitterShapeSettings;
import foundry.veil.api.quasar.particle.ParticleEmitter;
import foundry.veil.api.quasar.particle.ParticleSystemManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.client.renderer.SpellRenderStates;
import xyz.volcanobay.cabalist.client.renderer.circle.CircleShedding;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircle;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircleRenderer;
import xyz.volcanobay.cabalist.client.renderer.form.BarrierShellRenderer;
import xyz.volcanobay.cabalist.client.renderer.form.FormEmitterShape;
import xyz.volcanobay.cabalist.client.renderer.form.FormRenderer;
import xyz.volcanobay.cabalist.client.renderer.form.FormRenderers;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphRenderer;
import xyz.volcanobay.cabalist.client.visibility.ClientHidingSystem;
import xyz.volcanobay.cabalist.networking.packet.SpellVisualS2CPacket;
import xyz.volcanobay.cabalist.particle.GlyphParticleOptions;
import xyz.volcanobay.cabalist.system.render.FormShape;
import xyz.volcanobay.cabalist.system.render.Palette;
import xyz.volcanobay.cabalist.system.render.RenderLayer;
import xyz.volcanobay.cabalist.system.render.RenderSpec;
import xyz.volcanobay.cabalist.util.ColorHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Spell visuals on the client, kept until the server ends them. Every spell also gets rune circles and drifting glyphs.
 */
public class ClientSpellVisualSystem {
    public static final ClientSpellVisualSystem INSTANCE = new ClientSpellVisualSystem();

    private static final ResourceLocation BLOOM_PIPELINE = Cabalist.id("spell_bloom");
    private static final double MAX_VOLUME_SCALE = 200;
    private static final ResourceLocation DEFAULT_LAYER_SHADER = Cabalist.id("spell/glow");
    private static final ResourceLocation DEFAULT_LAYER_TEXTURE = Cabalist.id("textures/spell/glow.png");
    private static final float DEFAULT_LAYER_ALPHA = 0x90 / 255f;
    private static final double GLYPHS_PER_VOLUME = 0.08;
    private static final double MAX_GLYPHS_PER_TICK = 6;
    private static final double GLYPH_SPREAD = 1.5;
    private static final int PICK_SAMPLES = 16;
    private static final Map<LayerType, RenderType> LAYER_TYPES = new HashMap<>();

    private final List<ActiveVisual> visuals = new ArrayList<>();
    private final Int2ObjectMap<ActiveVisual> byId = new Int2ObjectOpenHashMap<>();
    private final MagicCircleRenderer arcane = new MagicCircleRenderer();
    private final List<RenderLayer> layers = new ArrayList<>();
    private final Vector3d glyphTarget = new Vector3d();
    private final List<MagicCircle> shedCircles = new ArrayList<>();

    public void add(SpellVisualS2CPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        end(packet.id());
        ActiveVisual visual = new ActiveVisual(packet.id(), packet.shape(), packet.specs(), packet.words(), packet.palette(), level);
        spawnEmitters(visual);
        visuals.add(visual);
        byId.put(packet.id(), visual);
    }

    public void setPalette(int id, Palette palette) {
        ActiveVisual visual = byId.get(id);
        if (visual != null) {
            visual.setSpoken(palette);
        }
    }

    public void update(int id, FormShape shape) {
        ActiveVisual visual = byId.get(id);
        if (visual == null) {
            return;
        }
        boolean isRespawnNeeded = visual.getShape().kind() != shape.kind() || visual.getShape().entityId() != shape.entityId();
        visual.setShape(shape);
        if (isRespawnNeeded) {
            removeEmitters(visual);
            spawnEmitters(visual);
        } else {
            for (ParticleEmitter emitter : visual.getEmitters()) {
                emitter.setPosition(shape.start());
            }
        }
    }

    // a beam channeled along its caster's facing.
    public double getChannel(int entityId, float partialTicks) {
        for (ActiveVisual visual : visuals) {
            FormShape shape = visual.getShape();
            if (shape.kind() == FormShape.Kind.BEAM && shape.entityId() == entityId && shape.height() > 0 && !visual.isEnding()) {
                return visual.getFade(partialTicks);
            }
        }
        return 0;
    }

    public record FormPick(int visualId, Vec3 point, double distance) {
    }

    public @Nullable FormPick pick(Vec3 from, Vec3 look, double range) {
        FormPick best = null;
        for (ActiveVisual visual : visuals) {
            if (visual.isEnding() || !visual.isPlaced() || visual.isSuppressed()) {
                continue;
            }
            Vec3 start = visual.getStart(1);
            Vec3 end = visual.getEnd(1);
            double width = Math.max(0.5, visual.getShape().radius());
            for (int i = 0; i <= PICK_SAMPLES; i++) {
                Vec3 point = start.lerp(end, i / (double) PICK_SAMPLES);
                double along = point.subtract(from).dot(look);
                if (along <= 0 || along > range) {
                    continue;
                }
                double miss = from.add(look.scale(along)).distanceTo(point);
                if (miss <= width && (best == null || along < best.distance())) {
                    best = new FormPick(visual.getId(), point, along);
                }
            }
        }
        return best;
    }

    public @Nullable Vec3 getCenter(int id) {
        ActiveVisual visual = byId.get(id);
        return visual == null || visual.isEnding() ? null : visual.getStart(1).lerp(visual.getEnd(1), 0.5);
    }

    public void setEnergy(int id, float flow, float remaining) {
        ActiveVisual visual = byId.get(id);
        if (visual != null) {
            visual.setEnergy(flow, remaining);
        }
    }

    public void end(int id) {
        ActiveVisual visual = byId.remove(id);
        if (visual == null) {
            return;
        }
        visual.beginEnding();
        for (ParticleEmitter emitter : visual.getEmitters()) {
            emitter.setCount(0);
        }
    }

    private static void removeEmitters(ActiveVisual visual) {
        for (ParticleEmitter emitter : visual.getEmitters()) {
            emitter.remove();
        }
        visual.getEmitters().clear();
    }

    private void spawnEmitters(ActiveVisual visual) {
        FormRenderer renderer = FormRenderers.get(visual.getShape().kind());
        double volume = Math.min(renderer.getVolume(visual), MAX_VOLUME_SCALE);
        for (RenderSpec spec : visual.getSpecs()) {
            for (ResourceLocation particle : spec.particles()) {
                spawnEmitter(visual, renderer, particle, volume);
            }
        }
    }

    private void spawnGlyphs(ActiveVisual visual) {
        FormRenderer renderer = FormRenderers.get(visual.getShape().kind());
        RandomSource random = visual.getLevel().getRandom();
        double expected = Math.min(MAX_GLYPHS_PER_TICK, renderer.getVolume(visual) * GLYPHS_PER_VOLUME);
        int count = (int) expected + (random.nextDouble() < expected - (int) expected ? 1 : 0);
        for (int i = 0; i < count; i++) {
            renderer.samplePoint(visual, random, 0, glyphTarget);
            GlyphParticleOptions options = new GlyphParticleOptions(visual.nextLetter(), true, visual.getPalette().sample(random.nextFloat(), GlyphRenderer.getTime()));
            visual.getLevel().addParticle(options, glyphTarget.x, glyphTarget.y, glyphTarget.z,
                    (random.nextDouble() - 0.5) * 2 * GLYPH_SPREAD, random.nextDouble() * GLYPH_SPREAD - 0.5, (random.nextDouble() - 0.5) * 2 * GLYPH_SPREAD);
        }
    }

    private @Nullable ParticleEmitter spawnEmitter(ActiveVisual visual, FormRenderer renderer, ResourceLocation id, double scale) {
        ParticleSystemManager manager = VeilRenderSystem.renderer().getParticleManager();
        ParticleEmitter emitter = manager.createEmitter(id);
        if (emitter == null) {
            return null;
        }
        emitter.setPosition(visual.getShape().start());
        Entity followed = visual.getEntity();
        if (followed != null) {
            emitter.setAttachedEntity(followed);
            visual.markEntitySeen();
        }
        emitter.setEmitterShapeSettings(List.of(new EmitterShapeSettings(new FormEmitterShape(renderer, visual), new Vector3f(1), new Vector3f(), false)));
        emitter.setCount(Math.max(1, (int) Math.round(emitter.getCount() * scale)));
        emitter.setMaxParticles((int) Math.max(1, Math.min(Integer.MAX_VALUE, emitter.getMaxParticles() * scale)));
        emitter.setLoop(true);
        manager.addParticleSystem(emitter);
        visual.getEmitters().add(emitter);
        return emitter;
    }

    public void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        for (Iterator<ActiveVisual> iterator = visuals.iterator(); iterator.hasNext(); ) {
            ActiveVisual visual = iterator.next();
            if (visual.getLevel() != level || visual.isExpired()) {
                removeEmitters(visual);
                if (byId.get(visual.getId()) == visual) {
                    byId.remove(visual.getId());
                }
                iterator.remove();
                continue;
            }
            visual.tick();
            Entity followed = visual.getEntity();
            if (followed != null && !visual.hasSeenEntity()) {
                visual.markEntitySeen();
                for (ParticleEmitter emitter : visual.getEmitters()) {
                    emitter.setAttachedEntity(followed);
                }
            }
            if (visual.getShape().followsEntity() && !visual.hasSeenEntity()) {
                continue;
            }
            boolean isHidden = ClientHidingSystem.isHidden(ClientHidingSystem.INSTANCE.getVisual(visual.getId()));
            if (isHidden != visual.isSuppressed()) {
                visual.setSuppressed(isHidden);
                if (isHidden) {
                    removeEmitters(visual);
                } else {
                    spawnEmitters(visual);
                }
            }
            if (isHidden) {
                continue;
            }
            if (!visual.isEnding() && visual.isPlaced()) {
                spawnGlyphs(visual);
                rumble(visual);
                shedFromCircles(visual);
            }
        }
    }

    private void shedFromCircles(ActiveVisual visual) {
        if (visual.getFlow() <= 0) {
            return;
        }
        shedCircles.clear();
        FormRenderers.get(visual.getShape().kind()).collectCircles(visual, 1, shedCircles);
        for (MagicCircle circle : shedCircles) {
            CircleShedding.shed(visual.getLevel(), circle, visual.getFlow(), visual.getPalette(), visual.getLevel().getRandom());
        }
    }

    // A running form shakes the screen by the amount of energy it's moving
    private static void rumble(ActiveVisual visual) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (visual.getFlow() <= 0 || !camera.isInitialized()) {
            return;
        }
        Vec3 start = visual.getStart(1);
        Vec3 end = visual.getEnd(1);
        Vec3 along = end.subtract(start);
        double length = along.lengthSqr();
        double t = length < 1e-6 ? 0 : Mth.clamp(camera.getPosition().subtract(start).dot(along) / length, 0, 1);
        SpellShake.add(start.add(along.scale(t)), visual.getFlow() / 20f);
    }

    public void render(MatrixStack pose, MultiBufferSource.BufferSource buffers, Camera camera, float partialTick) {
        for (ActiveVisual visual : visuals) {
            if (!visual.isPlaced() || visual.isSuppressed()) {
                continue;
            }
            FormRenderer renderer = FormRenderers.get(visual.getShape().kind());
            if (BarrierShellRenderer.isVoid(visual)) {
                BarrierShellRenderer.renderVoidForm(renderer, visual, pose, buffers, camera, partialTick);
            } else {
                for (RenderLayer layer : getLayers(visual, renderer)) {
                    renderLayer(renderer, visual, layer, pose, buffers, camera, partialTick, false);
                }
            }
            if (BarrierShellRenderer.isBarrier(visual)) {
                BarrierShellRenderer.render(visual, pose, buffers, camera, partialTick);
            }
        }
        buffers.endBatch();
        arcane.renderCircles(visuals, pose, buffers, camera, partialTick, false);
        renderBloom(pose, buffers, camera, partialTick);
    }

    private List<RenderLayer> getLayers(ActiveVisual visual, FormRenderer renderer) {
        layers.clear();
        for (RenderSpec spec : visual.getSpecs()) {
            layers.addAll(spec.layers());
        }
        float time = GlyphRenderer.getTime();
        if (layers.isEmpty() && renderer.needsDefaultLayer()) {
            layers.add(new RenderLayer(DEFAULT_LAYER_SHADER, DEFAULT_LAYER_TEXTURE, ColorHelper.withAlpha(visual.getPalette().sample(0, time), DEFAULT_LAYER_ALPHA), true));
        } else if (visual.hasSpokenColors()) {
            for (int i = 0; i < layers.size(); i++) {
                RenderLayer layer = layers.get(i);
                int color = ColorHelper.withAlpha(visual.getPalette().sample((float) i / layers.size(), time), ColorHelper.getAlpha(layer.color()));
                layers.set(i, new RenderLayer(layer.shader(), layer.texture(), color, layer.bloom(), layer.additive()));
            }
        }
        return layers;
    }

    private void renderBloom(MatrixStack pose, MultiBufferSource.BufferSource buffers, Camera camera, float partialTick) {
        AdvancedFbo bloom = VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(SpellRenderStates.BLOOM_FRAMEBUFFER);
        PostProcessingManager postProcessing = VeilRenderSystem.renderer().getPostProcessingManager();
        PostPipeline pipeline = postProcessing.getPipeline(BLOOM_PIPELINE);
        if (bloom == null || pipeline == null) {
            return;
        }
        bloom.clear(0, 0, 0, 0, GL11.GL_COLOR_BUFFER_BIT);
        AdvancedFbo.getMainFramebuffer().resolveToAdvancedFbo(bloom, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        for (ActiveVisual visual : visuals) {
            if (!visual.isPlaced() || visual.isSuppressed() || BarrierShellRenderer.isVoid(visual)) {
                continue;
            }
            FormRenderer renderer = FormRenderers.get(visual.getShape().kind());
            for (RenderLayer layer : getLayers(visual, renderer)) {
                if (layer.bloom()) {
                    renderLayer(renderer, visual, layer, pose, buffers, camera, partialTick, true);
                }
            }
        }
        buffers.endBatch();
        arcane.renderCircles(visuals, pose, buffers, camera, partialTick, true);
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        postProcessing.runPipeline(pipeline);
    }

    private void renderLayer(FormRenderer renderer, ActiveVisual visual, RenderLayer layer, MatrixStack pose, MultiBufferSource.BufferSource buffers, Camera camera,
                             float partialTick, boolean bloom) {
        float alpha = ClientHidingSystem.INSTANCE.getVisual(visual.getId());
        int color = alpha < 1 ? ColorHelper.withAlpha(layer.color(), ColorHelper.getAlpha(layer.color()) * alpha) : layer.color();
        RenderType type = LAYER_TYPES.computeIfAbsent(new LayerType(layer.shader(), layer.texture(), layer.additive(), bloom), ClientSpellVisualSystem::createLayerType);
        renderer.buildLayer(visual, buffers.getBuffer(type), pose, camera, partialTick, color);
    }

    private record LayerType(ResourceLocation shader, ResourceLocation texture, boolean additive, boolean bloom) {
    }

    private static RenderType createLayerType(LayerType type) {
        return RenderType.create("cabalist_spell_layer", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, RenderType.TRANSIENT_BUFFER_SIZE, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> {
                            // A layer whose shader failed to load still draws, plainly.
                            ShaderProgram shader = VeilRenderSystem.renderer().getShaderManager().getShader(type.shader());
                            return shader != null ? VeilRenderBridge.toShaderInstance(shader) : GameRenderer.getPositionTexColorShader();
                        }))
                        .setTextureState(new RenderStateShard.TextureStateShard(type.texture(), false, false))
                        .setTransparencyState(type.additive() ? RenderStateShard.LIGHTNING_TRANSPARENCY : SpellRenderStates.DEFAULT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setOutputState(SpellRenderStates.output(type.bloom()))
                        .createCompositeState(false));
    }
}
