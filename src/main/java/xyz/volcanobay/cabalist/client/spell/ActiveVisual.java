package xyz.volcanobay.cabalist.client.spell;

import foundry.veil.api.quasar.particle.ParticleEmitter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.client.renderer.circle.CircleEnergy;
import xyz.volcanobay.cabalist.system.render.FormShape;
import xyz.volcanobay.cabalist.system.render.Palette;
import xyz.volcanobay.cabalist.system.render.RenderSpec;

import java.util.ArrayList;
import java.util.List;

public class ActiveVisual {
    private boolean isSuppressed;
    private static final float FADE_IN_TICKS = 6;
    private static final float FADE_OUT_TICKS = 12;
    private static final float ENERGY_EASING = 0.2f;

    private final int id;
    private final String words;
    private int letterCursor;
    private float targetFlow;
    private float targetRemaining = 1;
    private float flow;
    private float remaining = 1;
    private float phase;
    private final List<RenderSpec> specs;
    private Palette spoken;
    private final ClientLevel level;
    private final List<ParticleEmitter> emitters = new ArrayList<>();
    private final boolean hasAppearance;
    private FormShape shape;
    private FormShape previousShape;
    private int shapeAge;
    private boolean hasSeenEntity;
    private int age;
    private int endAge = -1;

    public ActiveVisual(int id, FormShape shape, List<RenderSpec> specs, String words, Palette spoken, ClientLevel level) {
        this.id = id;
        this.words = words;
        this.shape = shape;
        this.previousShape = shape;
        this.specs = specs;
        this.spoken = spoken;
        this.level = level;
        boolean appearance = false;
        for (RenderSpec spec : specs) {
            if (!spec.isEmpty()) {
                appearance = true;
            }
        }
        this.hasAppearance = appearance;
    }

    public int getId() {
        return id;
    }

    public FormShape getShape() {
        return shape;
    }

    public void setShape(FormShape shape) {
        if (shape.entityId() != this.shape.entityId()) {
            hasSeenEntity = false;
        }
        previousShape = this.shape;
        shapeAge = age;
        this.shape = shape;
    }

    private float getShapeLerp(float partialTick) {
        return Math.min(1, age - shapeAge + partialTick);
    }

    public Vec3 getStart(float partialTick) {
        return previousShape.start().lerp(shape.start(), getShapeLerp(partialTick));
    }

    public Vec3 getEnd(float partialTick) {
        return previousShape.end().lerp(shape.end(), getShapeLerp(partialTick));
    }

    public List<RenderSpec> getSpecs() {
        return specs;
    }

    public Palette getPalette() {
        return spoken.or(Palette.of(RenderSpec.tintOf(specs)));
    }

    public void setSpoken(Palette spoken) {
        this.spoken = spoken;
    }

    public boolean hasSpokenColors() {
        return !spoken.isEmpty();
    }

    public String getWords() {
        return words;
    }

    public char nextLetter() {
        for (int tries = 0; tries < words.length(); tries++) {
            char letter = words.charAt(letterCursor++ % words.length());
            if (Character.isLetter(letter)) {
                return Character.toLowerCase(letter);
            }
        }
        return (char) ('a' + level.getRandom().nextInt(26));
    }

    public ClientLevel getLevel() {
        return level;
    }

    public List<ParticleEmitter> getEmitters() {
        return emitters;
    }

    public boolean hasAppearance() {
        return hasAppearance;
    }

    public int getAge() {
        return age;
    }

    public float getTime(float partialTick) {
        return age + partialTick;
    }

    public void tick() {
        age++;
        flow += (targetFlow - flow) * ENERGY_EASING;
        remaining += (targetRemaining - remaining) * ENERGY_EASING;
        phase += CircleEnergy.getSpin(flow);
    }

    public void setEnergy(float flow, float remaining) {
        this.targetFlow = flow;
        this.targetRemaining = remaining;
    }

    public float getFlow() {
        return flow;
    }

    public float getRemaining() {
        return remaining;
    }

    public float getPhase(float partialTick) {
        return phase + partialTick * CircleEnergy.getSpin(flow);
    }

    public float getProgress(float partialTick) {
        if (shape.duration() <= 0) {
            return 1;
        }
        return Math.min(1, (age + partialTick) / shape.duration());
    }

    public float getFade(float partialTick) {
        float fade = Math.min(1, (age + partialTick) / FADE_IN_TICKS);
        if (isEnding()) {
            fade *= Math.max(0, 1 - (age - endAge + partialTick) / FADE_OUT_TICKS);
        }
        return fade;
    }

    public boolean isSuppressed() {
        return isSuppressed;
    }

    public void setSuppressed(boolean suppressed) {
        isSuppressed = suppressed;
    }

    public boolean isEnding() {
        return endAge >= 0;
    }

    public void beginEnding() {
        if (endAge < 0) {
            endAge = age;
        }
    }

    public @Nullable Entity getEntity() {
        return shape.followsEntity() ? level.getEntity(shape.entityId()) : null;
    }

    public Vec3 getFollowedPosition(float partialTick) {
        Entity entity = getEntity();
        return entity == null ? shape.start() : entity.getPosition(partialTick);
    }

    public boolean isPlaced() {
        return !shape.followsEntity() || getEntity() != null;
    }

    public boolean hasSeenEntity() {
        return hasSeenEntity;
    }

    public void markEntitySeen() {
        hasSeenEntity = true;
    }

    public boolean isExpired() {
        return isEnding() && age - endAge > FADE_OUT_TICKS;
    }
}
