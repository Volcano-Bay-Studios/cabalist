package xyz.volcanobay.cabalist.system.protection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.render.SpellVisuals;
import xyz.volcanobay.cabalist.system.spell.Spell;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Claim system. Spells can claim areas in the world.
 */
public class ProtectionSystem {
    public static final ProtectionSystem INSTANCE = new ProtectionSystem();
    private static final long RENEW_TICKS = 100;
    private static final double BLOCKED_SHAKE_ENERGY = 4;

    private final Map<ResourceKey<Level>, Map<Long, Claim>> byLevel = new HashMap<>();

    private static class Claim {
        private long until;
        private long shookAt = -1;
        private final Set<UUID> allowed = new HashSet<>();
        private final Map<Spell, Long> renewedAt = new IdentityHashMap<>();
    }

    public boolean protect(Level level, BlockPos pos, Set<UUID> allowed, @Nullable Spell spell) {
        long now = level.getGameTime();
        Map<Long, Claim> claims = byLevel.computeIfAbsent(level.dimension(), key -> new HashMap<>());
        Claim claim = claims.computeIfAbsent(getColumn(pos), key -> new Claim());
        if (claim.until < now) {
            claim.allowed.clear();
            claim.renewedAt.clear();
        }
        claim.until = now + RENEW_TICKS;
        claim.allowed.addAll(allowed);
        if (spell == null) {
            return true;
        }
        Long renewed = claim.renewedAt.put(spell, now);
        return renewed == null || renewed != now;
    }

    public boolean isProtected(Level level, BlockPos pos) {
        return getClaim(level, pos) != null;
    }

    public boolean isAllowed(Level level, BlockPos pos, @Nullable Entity entity) {
        Claim claim = getClaim(level, pos);
        return claim == null || entity != null && claim.allowed.contains(entity.getUUID());
    }

    public boolean blocks(Level level, BlockPos pos, @Nullable Entity actor) {
        Claim claim = getClaim(level, pos);
        if (claim == null || actor != null && claim.allowed.contains(actor.getUUID())) {
            return false;
        }
        revealToAll(level, claim);
        shake(level, pos, claim);
        return true;
    }

    public boolean blocksExplosion(Level level, BlockPos pos, @Nullable Entity source) {
        Claim claim = getClaim(level, pos);
        if (claim == null) {
            return false;
        }
        revealToAll(level, claim);
        shake(level, pos, claim);
        return true;
    }

    private static void shake(Level level, BlockPos pos, Claim claim) {
        if (claim.shookAt != level.getGameTime()) {
            claim.shookAt = level.getGameTime();
            SpellVisuals.shake(level, new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), BLOCKED_SHAKE_ENERGY);
        }
    }

    public boolean reveal(Level level, BlockPos pos, Entity viewer) {
        Claim claim = getClaim(level, pos);
        if (claim == null) {
            return false;
        }
        reveal(level, claim, viewer);
        return true;
    }

    private static void revealToAll(Level level, Claim claim) {
        for (Spell spell : claim.renewedAt.keySet()) {
            spell.revealToAll(level.getGameTime());
        }
    }

    private static void reveal(Level level, Claim claim, @Nullable Entity actor) {
        if (actor != null) {
            for (Spell spell : claim.renewedAt.keySet()) {
                spell.getActedOn().put(actor.getUUID(), level.getGameTime());
            }
        }
    }

    private @Nullable Claim getClaim(Level level, BlockPos pos) {
        Map<Long, Claim> claims = byLevel.get(level.dimension());
        Claim claim = claims == null ? null : claims.get(getColumn(pos));
        return claim != null && claim.until >= level.getGameTime() && !isAbandoned(claim) ? claim : null;
    }

    private static boolean isAbandoned(Claim claim) {
        if (claim.renewedAt.isEmpty()) {
            return false;
        }
        for (Spell spell : claim.renewedAt.keySet()) {
            if (!spell.isDismissed()) {
                return false;
            }
        }
        return true;
    }

    public void prune(Level level) {
        Map<Long, Claim> claims = byLevel.get(level.dimension());
        if (claims == null) {
            return;
        }
        long now = level.getGameTime();
        claims.values().removeIf(claim -> claim.until < now);
        for (Claim claim : claims.values()) {
            claim.renewedAt.entrySet().removeIf(entry -> entry.getValue() + RENEW_TICKS < now || entry.getKey().isDismissed());
        }
    }

    private static long getColumn(BlockPos pos) {
        return (long) pos.getX() << 32 | pos.getZ() & 0xFFFFFFFFL;
    }

    public void clear() {
        byLevel.clear();
    }
}
