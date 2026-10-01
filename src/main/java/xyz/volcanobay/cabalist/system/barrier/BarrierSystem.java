package xyz.volcanobay.cabalist.system.barrier;

import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import xyz.volcanobay.cabalist.content.spell.aspect.BarrierAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.ProtectAspect;
import xyz.volcanobay.cabalist.core.CabalistAspects;
import xyz.volcanobay.cabalist.entity.SpellProjectile;
import xyz.volcanobay.cabalist.networking.packet.BarriersS2CPacket;
import xyz.volcanobay.cabalist.system.form.Delivery;
import xyz.volcanobay.cabalist.system.form.DeliverySystem;
import xyz.volcanobay.cabalist.system.render.FormShape;
import xyz.volcanobay.cabalist.system.spell.Spell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BarrierSystem {
    public static final BarrierSystem SERVER = new BarrierSystem();
    public static final BarrierSystem CLIENT = new BarrierSystem();

    private static final double PROJECTILE_BOUNCE = 0.25;

    private volatile Map<ResourceKey<Level>, List<Barrier>> byLevel = Map.of();
    private final Map<ResourceKey<Level>, List<Barrier>> synced = new HashMap<>();
    private final Map<Barrier, Barrier> located = new IdentityHashMap<>();
    private volatile Map<Barrier, Spell> spells = Map.of();
    private long locatedAt = Long.MIN_VALUE;

    public static BarrierSystem of(Level level) {
        return level.isClientSide ? CLIENT : SERVER;
    }

    public List<Barrier> get(ResourceKey<Level> dimension) {
        return byLevel.getOrDefault(dimension, List.of());
    }

    public void set(ResourceKey<Level> dimension, List<Barrier> barriers) {
        Map<ResourceKey<Level>, List<Barrier>> changed = new HashMap<>(byLevel);
        changed.put(dimension, List.copyOf(barriers));
        byLevel = changed;
    }

    public static Vec3 clip(Entity entity, Vec3 movement) {
        return of(entity.level()).clipAgainst(entity, movement);
    }

    public static void deflect(Projectile projectile) {
        if (!(projectile instanceof SpellProjectile)) {
            of(projectile.level()).deflectOff(projectile);
        }
    }

    private void deflectOff(Projectile projectile) {
        Vec3 velocity = projectile.getDeltaMovement();
        List<Barrier> barriers = get(projectile.level().dimension());
        if (barriers.isEmpty() || velocity.lengthSqr() < 1e-12) {
            return;
        }
        Vec3 from = projectile.position().add(0, projectile.getBbHeight() / 2, 0);
        double margin = projectile.getBbWidth() / 2;
        for (Barrier barrier : barriers) {
            if (barrier.allows(projectile)) {
                continue;
            }
            Vec3 deflected = locate(barrier, projectile.level()).deflect(from, velocity, margin, PROJECTILE_BOUNCE);
            if (deflected != null) {
                velocity = deflected;
                projectile.setDeltaMovement(velocity);
                projectile.hasImpulse = true;
                charge(barrier);
            }
        }
    }

    private Barrier locate(Barrier barrier, Level level) {
        if (level.getGameTime() != locatedAt) {
            located.clear();
            locatedAt = level.getGameTime();
        }
        return located.computeIfAbsent(barrier, unlocated -> unlocated.locate(level));
    }

    private Vec3 clipAgainst(Entity entity, Vec3 movement) {
        if (movement.lengthSqr() < 1e-12) {
            return movement;
        }
        Level level = entity.level();
        List<Barrier> barriers = get(level.dimension());
        if (barriers.isEmpty()) {
            return movement;
        }
        Vec3 from = entity.position().add(0, entity.getBbHeight() / 2, 0);
        double margin = entity.getBbWidth() / 2;
        for (Barrier barrier : barriers) {
            if (!barrier.allows(entity)) {
                Vec3 clipped = locate(barrier, level).clip(from, movement, margin);
                if (!clipped.equals(movement)) {
                    charge(barrier);
                }
                movement = clipped;
            }
        }
        return movement;
    }

    private void charge(Barrier barrier) {
        Spell spell = spells.get(barrier);
        if (spell != null && CabalistAspects.BARRIER.get() instanceof BarrierAspect aspect) {
            aspect.chargeBlock(spell);
        }
    }

    public void tickServer(MinecraftServer server) {
        Map<ResourceKey<Level>, List<Barrier>> found = new HashMap<>();
        Map<Barrier, Spell> owners = new IdentityHashMap<>();
        for (Delivery delivery : DeliverySystem.INSTANCE.getActive()) {
            if (!BarrierAspect.isBarrier(delivery.getClause())) {
                continue;
            }
            List<UUID> allowed = List.copyOf(ProtectAspect.getAllowed(delivery.getClause(), delivery.getLevel()));
            for (FormShape shape : delivery.getShapes()) {
                Barrier barrier = Barrier.of(shape, allowed);
                found.computeIfAbsent(delivery.getLevel().dimension(), key -> new ArrayList<>()).add(barrier);
                owners.put(barrier, delivery.getSpell());
            }
        }
        byLevel = found;
        spells = owners;
        for (ServerLevel level : server.getAllLevels()) {
            List<Barrier> barriers = get(level.dimension());
            if (!barriers.equals(synced.getOrDefault(level.dimension(), List.of()))) {
                synced.put(level.dimension(), barriers);
                VeilPacketManager.level(level).sendPacket(new BarriersS2CPacket(level.dimension(), barriers));
            }
        }
    }

    public void sendTo(ServerPlayer player) {
        VeilPacketManager.player(player).sendPacket(new BarriersS2CPacket(player.level().dimension(), get(player.level().dimension())));
    }

    public void clear() {
        byLevel = Map.of();
        synced.clear();
        located.clear();
        spells = Map.of();
    }
}
