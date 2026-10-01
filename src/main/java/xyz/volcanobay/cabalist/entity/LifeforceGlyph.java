package xyz.volcanobay.cabalist.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistEntities;
import xyz.volcanobay.cabalist.system.focus.Focus;
import xyz.volcanobay.cabalist.system.focus.LifeforceFlow;

public class LifeforceGlyph extends Entity {
    private static final EntityDataAccessor<Integer> CHARACTER = SynchedEntityData.defineId(LifeforceGlyph.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(LifeforceGlyph.class, EntityDataSerializers.INT);

    private static final int SLAIN_LIFETIME = 100;
    private static final int RELEASED_LIFETIME = 600;
    private static final double LIFETIME_VARIATION = 0.4;
    private static final int FADE_IN_TICKS = 5;
    private static final int FADE_OUT_TICKS = 30;
    private static final double LIFEFORCE_PER_GLYPH = 10;
    private static final int MAX_GLYPHS = 8;

    private static final int DRIFT_TICKS = 15;
    private static final int SEARCH_TICKS = 10;
    private static final double REACH = 8;
    private static final double ABSORB_DISTANCE = 0.5;
    private static final double PULL = 0.04;
    private static final double DRAG = 0.9;
    private static final double WANDER = 0.006;
    private static final double RELEASED_WANDER = 0.012;
    private static final double BOB = 0.002;
    private static final double EMPTY = 1e-3;

    private ResourceLocation type = ResourceLocation.withDefaultNamespace("pig");
    private double amount;
    private boolean released;
    private @Nullable Entity target;

    public LifeforceGlyph(EntityType<? extends LifeforceGlyph> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static void scatter(ServerLevel level, Vec3 at, ResourceLocation type, double amount, boolean released) {
        int count = Mth.clamp((int) Math.ceil(amount / LIFEFORCE_PER_GLYPH), 1, MAX_GLYPHS);
        for (int i = 0; i < count; i++) {
            LifeforceGlyph glyph = new LifeforceGlyph(CabalistEntities.LIFEFORCE_GLYPH.get(), level);
            glyph.type = type;
            glyph.amount = amount / count;
            glyph.released = released;
            int lifetime = released ? RELEASED_LIFETIME : SLAIN_LIFETIME;
            glyph.entityData.set(LIFETIME, (int) (lifetime * (1 + LIFETIME_VARIATION * (level.random.nextDouble() - 0.5))));
            glyph.entityData.set(CHARACTER, 'a' + level.random.nextInt(26));
            glyph.setPos(at);
            glyph.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.3, 0.1 + level.random.nextDouble() * 0.15, (level.random.nextDouble() - 0.5) * 0.3);
            level.addFreshEntity(glyph);
        }
    }

    public char getCharacter() {
        return (char) (int) entityData.get(CHARACTER);
    }

    public float getFade(float partialTick) {
        float age = tickCount + partialTick;
        float in = Mth.clamp(age / FADE_IN_TICKS, 0, 1);
        float out = Mth.clamp((entityData.get(LIFETIME) - age) / FADE_OUT_TICKS, 0, 1);
        return Math.min(in, out);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(CHARACTER, (int) 'a');
        builder.define(LIFETIME, SLAIN_LIFETIME);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount >= entityData.get(LIFETIME) || amount <= EMPTY) {
            discard();
            return;
        }
        if (target != null && !wants(target)) {
            target = null;
        }
        if (tickCount >= DRIFT_TICKS && tickCount % SEARCH_TICKS == 0) {
            target = findTarget();
        }
        Vec3 motion = getDeltaMovement().scale(DRAG);
        if (target != null) {
            Vec3 toward = getCenter(target).subtract(position());
            if (toward.length() < ABSORB_DISTANCE) {
                absorbInto(target);
            } else {
                motion = motion.add(toward.normalize().scale(PULL));
            }
        } else {
            double wander = released ? RELEASED_WANDER : WANDER;
            motion = motion.add(random.nextGaussian() * wander, random.nextGaussian() * wander + Mth.sin(tickCount * 0.15f) * BOB, random.nextGaussian() * wander);
        }
        setDeltaMovement(motion);
        setPos(position().add(motion));
    }

    private @Nullable Entity findTarget() {
        Entity nearest = null;
        double best = REACH * REACH;
        for (Entity entity : level().getEntities(this, getBoundingBox().inflate(REACH), this::wants)) {
            double distance = entity.distanceToSqr(this);
            if (distance < best) {
                nearest = entity;
                best = distance;
            }
        }
        return nearest;
    }

    private boolean wants(Entity entity) {
        if (!entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (entity instanceof ItemEntity item) {
            return LifeforceFlow.attracts(item.getItem(), type, true);
        }
        return entity instanceof LivingEntity living && LifeforceFlow.attracts(living, type);
    }

    private static Vec3 getCenter(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() / 2, 0);
    }

    private void absorbInto(Entity entity) {
        double taken;
        if (entity instanceof ItemEntity item) {
            ItemStack stack = item.getItem().copy();
            taken = Focus.give(stack, type, amount);
            item.setItem(stack);
        } else {
            taken = LifeforceFlow.absorb((LivingEntity) entity, type, amount);
        }
        amount -= taken;
        target = null;
        if (taken > 0) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.3f, 1.2f + random.nextFloat() * 0.6f);
        }
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        ResourceLocation saved = ResourceLocation.tryParse(tag.getString("type"));
        if (saved != null) {
            type = saved;
        }
        amount = tag.getDouble("amount");
        released = tag.getBoolean("released");
        tickCount = tag.getInt("age");
        entityData.set(LIFETIME, tag.getInt("lifetime"));
        entityData.set(CHARACTER, tag.getInt("character"));
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putString("type", type.toString());
        tag.putDouble("amount", amount);
        tag.putBoolean("released", released);
        tag.putInt("age", tickCount);
        tag.putInt("lifetime", entityData.get(LIFETIME));
        tag.putInt("character", entityData.get(CHARACTER));
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
