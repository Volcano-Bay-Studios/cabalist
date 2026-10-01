package xyz.volcanobay.cabalist.client.spell;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.client.renderer.circle.CircleClearance;
import xyz.volcanobay.cabalist.client.renderer.circle.CircleEnergy;
import xyz.volcanobay.cabalist.client.renderer.circle.CircleShedding;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircle;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircleRenderer;
import xyz.volcanobay.cabalist.client.visibility.ClientHidingSystem;
import xyz.volcanobay.cabalist.networking.packet.HangingSpellsS2CPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ClientHangingSpellSystem {
    public static final ClientHangingSpellSystem INSTANCE = new ClientHangingSpellSystem();

    // Big enough for one ring of runes.
    private static final float CIRCLE_RADIUS = 1.25f;
    private static final float CIRCLE_GAP = 0.35f;
    private static final float HOST_MARGIN = 0.4f;
    private static final float ORBIT_SPEED = 0.004f;
    private static final float WAITING_BRIGHTNESS = 0.45f;
    private static final float READY_BRIGHTNESS = 0.75f;
    private static final float READY_PULSE = 0.25f;
    private static final float READY_PULSE_SPEED = 0.2f;
    private static final float APPEAR_TICKS = 10;
    private static final float LEAVE_TICKS = 12;
    private static final long NOT_LEAVING = -1;
    private static final float CHARGE_EASE_TICKS = 10;
    private static final float FLOAT_MIN_HEIGHT = 0.1f;
    private static final float FLOAT_HEIGHT_RANGE = 0.9f;
    private static final float BOB_HEIGHT = 0.12f;
    private static final float BOB_SPEED = 0.03f;
    private static final float MOTION_EASE = 0.12f;

    private final Int2ObjectMap<List<Shown>> hangingByEntity = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<List<Shown>> inscribedByEntity = new Int2ObjectOpenHashMap<>();
    private final Map<BlockPos, List<Shown>> inscribedByBlock = new HashMap<>();
    private final List<Shown> combined = new ArrayList<>();
    private final List<MagicCircleRenderer.CircleDraw> scratch = new ArrayList<>();
    private @Nullable ClientLevel level;

    public void set(int entityId, List<HangingSpellsS2CPacket.Entry> spells) {
        if (checkLevel()) {
            store(hangingByEntity, entityId, spells);
        }
    }

    public void setInscribedOnEntity(int entityId, List<HangingSpellsS2CPacket.Entry> spells) {
        if (checkLevel()) {
            store(inscribedByEntity, entityId, spells);
        }
    }

    public void setInscribedOnBlock(BlockPos pos, List<HangingSpellsS2CPacket.Entry> spells) {
        if (checkLevel()) {
            store(inscribedByBlock, pos.immutable(), spells);
        }
    }

    private boolean checkLevel() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != level) {
            hangingByEntity.clear();
            inscribedByEntity.clear();
            inscribedByBlock.clear();
            level = current;
        }
        return current != null;
    }

    private <K> void store(Map<K, List<Shown>> map, K key, List<HangingSpellsS2CPacket.Entry> spells) {
        long now = level == null ? 0 : level.getGameTime();
        List<Shown> unclaimed = new ArrayList<>(map.getOrDefault(key, List.of()));
        List<Shown> shown = new ArrayList<>(spells.size() + unclaimed.size());
        for (HangingSpellsS2CPacket.Entry entry : spells) {
            long appearedAt = now;
            float chargeFrom = entry.charge();
            float phase = 0;
            Motion motion = new Motion();
            for (Shown old : unclaimed) {
                if (old.entry.words().equals(entry.words())) {
                    appearedAt = old.leftAt == NOT_LEAVING ? old.appearedAt : now - Math.round(old.getFade(now) * APPEAR_TICKS);
                    chargeFrom = old.getCharge(now);
                    phase = old.getPhase(now);
                    motion = old.motion;
                    unclaimed.remove(old);
                    break;
                }
            }
            shown.add(new Shown(entry, appearedAt, NOT_LEAVING, chargeFrom, now, phase, motion));
        }
        for (Shown old : unclaimed) {
            shown.add(old.leftAt == NOT_LEAVING ? new Shown(old.entry, old.appearedAt, now, old.chargeFrom, old.chargeChangedAt, old.phase, old.motion) : old);
        }
        if (shown.isEmpty()) {
            map.remove(key);
        } else {
            map.put(key, shown);
        }
    }

    private static <K> void pruneLeft(Map<K, List<Shown>> map, float time) {
        map.values().forEach(spells -> spells.removeIf(spell -> spell.leftAt != NOT_LEAVING && time - spell.leftAt >= LEAVE_TICKS));
        map.values().removeIf(List::isEmpty);
    }

    public void collectCircles(float partialTick, List<MagicCircleRenderer.CircleDraw> out) {
        if (!checkLevel() || level == null) {
            return;
        }
        float time = level.getGameTime() + partialTick;
        pruneLeft(hangingByEntity, time);
        pruneLeft(inscribedByEntity, time);
        pruneLeft(inscribedByBlock, time);
        forEachHost(partialTick, (spells, center, minimumOrbit, visibility) -> arrange(spells, center, minimumOrbit, time, out, visibility));
    }

    public void tick() {
        if (!checkLevel() || level == null) {
            return;
        }
        float time = level.getGameTime();
        forEachHost(1, (spells, center, minimumOrbit, visibility) -> shed(spells, center, minimumOrbit, time, visibility));
    }

    private interface HostVisitor {
        void visit(List<Shown> spells, Vec3 center, float minimumOrbit, Function<Shown, Float> visibility);
    }

    private void forEachHost(float partialTick, HostVisitor visitor) {
        for (Entity entity : level.entitiesForRendering()) {
            List<Shown> hanging = hangingByEntity.get(entity.getId());
            List<Shown> inscribed = inscribedByEntity.get(entity.getId());
            if (hanging == null && inscribed == null) {
                continue;
            }
            combined.clear();
            if (hanging != null) {
                combined.addAll(hanging);
            }
            if (inscribed != null) {
                combined.addAll(inscribed);
            }
            int host = entity.getId();
            visitor.visit(combined, getFeet(entity, partialTick), getMinimumOrbit(entity), spell -> ClientHidingSystem.INSTANCE.getCircle(host, spell.entry.words()));
        }
        for (Map.Entry<BlockPos, List<Shown>> entry : inscribedByBlock.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!level.isLoaded(pos)) {
                continue;
            }
            VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
            double top = shape.isEmpty() ? 0 : shape.max(Direction.Axis.Y);
            Vec3 surface = new Vec3(pos.getX() + 0.5, pos.getY() + top + MagicCircle.GROUND_OFFSET, pos.getZ() + 0.5);
            float minimumOrbit = entry.getValue().size() == 1 ? 0 : CIRCLE_RADIUS + HOST_MARGIN;
            visitor.visit(entry.getValue(), surface, minimumOrbit, spell -> ClientHidingSystem.INSTANCE.getBlockCircle(pos, spell.entry.words()));
        }
    }

    private void shed(List<Shown> spells, Vec3 center, float minimumOrbit, float time, Function<Shown, Float> visibility) {
        scratch.clear();
        arrange(spells, center, minimumOrbit, time, scratch, visibility);
        for (int i = 0; i < scratch.size(); i++) {
            HangingSpellsS2CPacket.Entry entry = spells.get(i).entry;
            if (entry.flow() > 0 && scratch.get(i).fade() > 0) {
                CircleShedding.shed(level, scratch.get(i).circle(), entry.flow(), entry.palette(), level.getRandom());
            }
        }
    }

    public @Nullable CirclePick pick(Vec3 from, Vec3 look, double range, float partialTick) {
        if (!checkLevel() || level == null || Math.abs(look.y) < 1e-4) {
            return null;
        }
        float time = level.getGameTime() + partialTick;
        CirclePick best = null;
        for (Int2ObjectMap.Entry<List<Shown>> entry : hangingByEntity.int2ObjectEntrySet()) {
            Entity entity = level.getEntity(entry.getIntKey());
            if (entity == null) {
                continue;
            }
            List<Shown> hanging = entry.getValue();
            int count = getCircleCount(entity.getId());
            float orbit = getOrbit(count, getMinimumOrbit(entity));
            int index = 0;
            for (int i = 0; i < hanging.size(); i++) {
                Shown spell = hanging.get(i);
                if (spell.leftAt != NOT_LEAVING) {
                    continue;
                }
                if (ClientHidingSystem.isHidden(ClientHidingSystem.INSTANCE.getCircle(entity.getId(), spell.entry.words()))) {
                    index++;
                    continue;
                }
                Vec3 at = place(spell, i, count, getFeet(entity, partialTick), orbit, time);
                double distance = new MagicCircle(at, MagicCircle.UP, CIRCLE_RADIUS).getHitDistance(from, look);
                if (distance >= 0 && distance <= range && (best == null || distance < best.distance())) {
                    best = new CirclePick(entity.getId(), index, spell.entry.words().hashCode(), distance);
                }
                index++;
            }
        }
        return best;
    }

    public @Nullable Vec3 getCircleCenter(int entityId, int index, float partialTick) {
        List<Shown> hanging = hangingByEntity.get(entityId);
        Entity entity = level == null ? null : level.getEntity(entityId);
        if (hanging == null || entity == null) {
            return null;
        }
        float time = level.getGameTime() + partialTick;
        int count = getCircleCount(entityId);
        int remaining = index;
        for (int i = 0; i < hanging.size(); i++) {
            Shown spell = hanging.get(i);
            if (spell.leftAt == NOT_LEAVING && remaining-- == 0) {
                return place(spell, i, count, getFeet(entity, partialTick), getOrbit(count, getMinimumOrbit(entity)), time);
            }
        }
        return null;
    }

    public static float getCircleRadius() {
        return CIRCLE_RADIUS;
    }

    private int getCircleCount(int entityId) {
        return hangingByEntity.getOrDefault(entityId, List.of()).size() + inscribedByEntity.getOrDefault(entityId, List.of()).size();
    }

    private static Vec3 getFeet(Entity entity, float partialTick) {
        return entity.getPosition(partialTick).add(0, MagicCircle.GROUND_OFFSET, 0);
    }

    private static float getMinimumOrbit(Entity entity) {
        return entity.getBbWidth() / 2 + CIRCLE_RADIUS + HOST_MARGIN;
    }

    private static float getOrbit(int count, float minimumOrbit) {
        return count == 1 && minimumOrbit == 0 ? 0 : Math.max(minimumOrbit, count * (2 * CIRCLE_RADIUS + CIRCLE_GAP) / Mth.TWO_PI);
    }

    private static Vec3 place(Shown spell, int i, int count, Vec3 center, float orbit, float time) {
        Motion motion = spell.motion;
        motion.ease(Mth.TWO_PI * i / count, orbit, time);
        float angle = motion.slot + time * ORBIT_SPEED;
        float height = getFloatHeight(spell.entry.words().hashCode() & MagicCircle.SEED_MASK, time);
        Vec3 at = center.add(Mth.cos(angle) * motion.orbit, height, Mth.sin(angle) * motion.orbit);
        return CircleClearance.lift(motion, at, MagicCircle.UP, CIRCLE_RADIUS, false);
    }

    private static void arrange(List<Shown> spells, Vec3 center, float minimumOrbit, float time, List<MagicCircleRenderer.CircleDraw> out,
                                Function<Shown, Float> visibility) {
        int count = spells.size();
        float orbit = getOrbit(count, minimumOrbit);
        for (int i = 0; i < count; i++) {
            Shown spell = spells.get(i);
            int seed = spell.entry.words().hashCode() & MagicCircle.SEED_MASK;
            Vec3 at = place(spell, i, count, center, orbit, time);
            float fade = spell.getFade(time) * visibility.apply(spell);
            out.add(new MagicCircleRenderer.CircleDraw(new MagicCircle(at, MagicCircle.UP, CIRCLE_RADIUS), seed,
                    spell.getPhase(time), fade, getBrightness(spell.entry, time), spell.entry.palette(), spell.entry.words(), spell.getCharge(time), spell.entry.awaiting(),
                    isArbiter(spell.entry)));
        }
    }

    private static boolean isArbiter(HangingSpellsS2CPacket.Entry entry) {
        Player player = Minecraft.getInstance().player;
        return player != null && entry.arbiter().map(player.getUUID()::equals).orElse(false);
    }

    private static float getFloatHeight(int seed, float time) {
        float rest = FLOAT_MIN_HEIGHT + FLOAT_HEIGHT_RANGE * (seed * 0.618034f % 1);
        return rest + BOB_HEIGHT * Mth.sin(time * BOB_SPEED + seed);
    }

    private static float getBrightness(HangingSpellsS2CPacket.Entry entry, float time) {
        if (entry.running()) {
            return CircleEnergy.getBrightness(entry.flow(), entry.remaining(), time);
        }
        if (entry.charge() >= 1) {
            return READY_BRIGHTNESS + READY_PULSE * Mth.sin(time * READY_PULSE_SPEED);
        }
        return WAITING_BRIGHTNESS;
    }

    public record CirclePick(int entityId, int index, int wordsHash, double distance) {
    }

    private static class Motion {
        private float slot;
        private float orbit;
        private float at = Float.NaN;

        private void ease(float targetSlot, float targetOrbit, float time) {
            if (Float.isNaN(at)) {
                slot = targetSlot;
                orbit = targetOrbit;
                at = time;
                return;
            }
            float ticks = time - at;
            if (ticks <= 0) {
                return;
            }
            float amount = 1 - (float) Math.exp(-ticks * MOTION_EASE);
            slot += Mth.wrapDegrees((targetSlot - slot) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD * amount;
            orbit += (targetOrbit - orbit) * amount;
            at = time;
        }
    }

    private record Shown(HangingSpellsS2CPacket.Entry entry, long appearedAt, long leftAt, float chargeFrom, long chargeChangedAt, float phase, Motion motion) {
        private float getPhase(float time) {
            return phase + (time - chargeChangedAt) * CircleEnergy.getSpin(entry.flow());
        }

        private float getCharge(float time) {
            return Mth.lerp(Mth.clamp((time - chargeChangedAt) / CHARGE_EASE_TICKS, 0, 1), chargeFrom, entry.charge());
        }

        private float getFade(float time) {
            float fade = Mth.clamp((time - appearedAt) / APPEAR_TICKS, 0, 1);
            if (leftAt != NOT_LEAVING) {
                fade *= Mth.clamp(1 - (time - leftAt) / LEAVE_TICKS, 0, 1);
            }
            return fade;
        }
    }
}
