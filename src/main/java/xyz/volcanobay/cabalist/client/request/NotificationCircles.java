package xyz.volcanobay.cabalist.client.request;

import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.client.casting.ClientCastingSystem;
import xyz.volcanobay.cabalist.client.renderer.circle.CircleClearance;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircle;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircleRenderer;
import xyz.volcanobay.cabalist.client.renderer.circle.Occlusion;
import xyz.volcanobay.cabalist.client.visibility.ClientHidingSystem;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.networking.packet.RequestActionC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.RequestsS2CPacket;
import xyz.volcanobay.cabalist.system.render.Palette;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Request circles float around their player and face them. Only the local player's own circles can be charged open, dragged or nudged.
public class NotificationCircles {
    public static final NotificationCircles INSTANCE = new NotificationCircles();

    // do not touch evil constants
    private static final float ORBIT = 2f;
    private static final float UNREAD_RADIUS = 0.5f;
    private static final float READ_RADIUS = 0.35f;
    private static final float REST_HEIGHT = -0.2f;
    private static final float MIN_HEIGHT = -1.2f;
    private static final float MAX_HEIGHT = 0.8f;
    private static final float FIRST_SLOT_DEGREES = 35;
    private static final float SLOT_DEGREES = 30;
    private static final float NUDGE_MIN_DEGREES = 25;
    private static final float NUDGE_MAX_DEGREES = 60;
    private static final float NUDGE_HEIGHT = 0.3f;
    private static final float PLACE_EASE = 0.15f;
    private static final float OPEN_EASE = 0.12f;
    private static final float OPEN_DROP = 0.9f;
    private static final float APPEAR_TICKS = 10;
    private static final float UNREAD_BOB = 0.06f;
    private static final float READ_BOB = 0.02f;
    private static final float BOB_SPEED = 0.05f;
    private static final float UNREAD_BRIGHTNESS = 0.8f;
    private static final float UNREAD_PULSE = 0.25f;
    private static final float PULSE_SPEED = 0.15f;
    private static final float SHINE_INTERVAL_TICKS = 60;
    private static final float SHINE_TICKS = 8;
    private static final float SHINE = 0.6f;
    private static final float READ_BRIGHTNESS = 0.4f;
    private static final float READ_FADE = 0.6f;
    private static final float WORLD_RING_SCALE = 1.3f;
    private static final int WORLD_RING_SEED = 257;
    private static final int WORLD_RING_TINT = 0xFFFFC94A;
    private static final double TICK_SECONDS = 0.05;

    private final Map<Key, Placement> placements = new HashMap<>();
    private final RandomSource random = RandomSource.create();
    private @Nullable ClientLevel level;
    private @Nullable Key charging;
    private double chargeSeconds;
    private float charge;
    private float prevCharge;
    private @Nullable Key dragging;
    private final LinkedHashSet<Key> open = new LinkedHashSet<>();
    private int age;

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != level) {
            level = minecraft.level;
            placements.clear();
            charging = dragging = null;
            open.clear();
        }
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        age++;
        List<RequestsS2CPacket.Entry> own = ClientRequestSystem.INSTANCE.get(player.getId());
        placements.keySet().removeIf(key -> own.stream().noneMatch(key::matches));
        for (RequestsS2CPacket.Entry entry : own) {
            Key key = Key.of(entry);
            if (!placements.containsKey(key)) {
                placements.put(key, createPlacement(player, entry));
                if (entry.role() == RequestsS2CPacket.ROLE_INFO) {
                    open.add(key);
                }
            }
        }
        open.retainAll(placements.keySet());
        for (Map.Entry<Key, Placement> entry : placements.entrySet()) {
            entry.getValue().tick(open.contains(entry.getKey()));
        }
        tickControls(minecraft, player);
    }

    private void tickControls(Minecraft minecraft, LocalPlayer player) {
        boolean isUseDown = minecraft.options.keyUse.isDown();
        if (dragging != null) {
            Placement placement = placements.get(dragging);
            if (!isUseDown || placement == null) {
                dragging = null;
            } else {
                Vec3 look = player.getViewVector(1);
                placement.moveTo(player.getYRot(), Mth.clamp((float) look.y * ORBIT, MIN_HEIGHT, MAX_HEIGHT));
            }
        }
        prevCharge = charge;
        if (charging != null) {
            if (!isUseDown || !placements.containsKey(charging)) {
                charging = null;
            } else {
                chargeSeconds += TICK_SECONDS;
                charge = (float) Math.min(1, chargeSeconds / CabalistConfig.STANDARD_CHARGE_SECONDS.get());
                if (charge >= 1) {
                    openCircle(charging);
                    charging = null;
                }
            }
        }
        if (charging == null) {
            charge = 0;
        }
    }

    // An appraisal sits on the side facing what it describes, and as near its height as the circles can float
    private Placement createPlacement(LocalPlayer player, RequestsS2CPacket.Entry entry) {
        Vec3 about = getAbout(entry);
        if (about == null) {
            return new Placement(player.getYRot() + FIRST_SLOT_DEGREES + SLOT_DEGREES * placements.size(), REST_HEIGHT, age);
        }
        Vec3 toward = about.subtract(player.getEyePosition());
        double horizontal = Math.sqrt(toward.x * toward.x + toward.z * toward.z);
        float height = horizontal < 1e-3 ? REST_HEIGHT : (float) (toward.y / horizontal * ORBIT);
        return new Placement((float) (Mth.atan2(-toward.x, toward.z) * Mth.RAD_TO_DEG), Mth.clamp(height, MIN_HEIGHT, MAX_HEIGHT), age);
    }

    private static @Nullable Vec3 getAbout(RequestsS2CPacket.Entry entry) {
        if (entry.role() != RequestsS2CPacket.ROLE_INFO || entry.args().size() < 4) {
            return null;
        }
        return new Vec3(Double.parseDouble(entry.args().get(1)), Double.parseDouble(entry.args().get(2)), Double.parseDouble(entry.args().get(3)));
    }

    private void openCircle(Key key) {
        open.remove(key);
        open.add(key);
        KeyMapping.releaseAll();
        VeilPacketManager.server().sendPacket(new RequestActionC2SPacket(key.id(), RequestActionC2SPacket.OPEN));
    }

    private void close(Key key) {
        if (open.remove(key)) {
            VeilPacketManager.server().sendPacket(new RequestActionC2SPacket(key.id(), RequestActionC2SPacket.CLOSE));
        }
    }

    public void close(RequestsS2CPacket.Entry entry) {
        close(Key.of(entry));
    }

    public void closeLatest() {
        Key latest = null;
        for (Key key : open) {
            latest = key;
        }
        if (latest != null) {
            close(latest);
        }
    }

    public boolean hasOpen() {
        return !open.isEmpty();
    }

    public boolean isOpen(Entity host, RequestsS2CPacket.Entry entry) {
        return host == Minecraft.getInstance().player ? open.contains(Key.of(entry)) : entry.isOpen();
    }

    // Typing goes to the last opened circle that's waiting
    public @Nullable RequestsS2CPacket.Entry getAnsweringEntry() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        RequestsS2CPacket.Entry latest = null;
        for (Key key : open) {
            for (RequestsS2CPacket.Entry entry : ClientRequestSystem.INSTANCE.get(player.getId())) {
                if (key.matches(entry) && entry.role() == RequestsS2CPacket.ROLE_ANSWER) {
                    latest = entry;
                }
            }
        }
        return latest;
    }

    public void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || ClientCastingSystem.INSTANCE.isCasting()) {
            return;
        }
        if (event.isUseItem()) {
            if (charging != null || dragging != null) {
                cancel(event);
                return;
            }
            Pick pick = pick(player, 1, false);
            if (pick == null || ClientCastingSystem.isFocus(player.getMainHandItem()) || isItemUsed(minecraft, player, pick.distance())) {
                return;
            }
            cancel(event);
            if (player.isShiftKeyDown()) {
                dragging = pick.key();
            } else if (open.contains(pick.key())) {
                close(pick.key());
            } else {
                charging = pick.key();
                chargeSeconds = 0;
            }
        } else if (event.isAttack()) {
            Pick pick = pick(player, 1, false);
            if (pick == null || isTargetCloser(minecraft, player, pick.distance())) {
                return;
            }
            if (player.isShiftKeyDown()) {
                cancel(event);
                VeilPacketManager.server().sendPacket(new RequestActionC2SPacket(pick.key().id(), RequestActionC2SPacket.DISMISS));
            } else {
                nudge(placements.get(pick.key()));
            }
        }
    }

    public boolean isDragging() {
        return dragging != null;
    }

    public boolean tryStartDrag(LocalPlayer player) {
        Pick pick = pick(player, 1, true);
        if (pick == null) {
            return false;
        }
        dragging = pick.key();
        return true;
    }

    private static void cancel(InputEvent.InteractionKeyMappingTriggered event) {
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    private static boolean isItemUsed(Minecraft minecraft, LocalPlayer player, double circleDistance) {
        for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (stack.getUseAnimation() != UseAnim.NONE) {
                return true;
            }
        }
        return player.getMainHandItem().getItem() instanceof BlockItem && isTargetCloser(minecraft, player, circleDistance);
    }

    private static boolean isTargetCloser(Minecraft minecraft, LocalPlayer player, double circleDistance) {
        HitResult hit = minecraft.hitResult;
        return hit != null && hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceTo(player.getEyePosition()) < circleDistance;
    }

    private void nudge(@Nullable Placement placement) {
        if (placement == null) {
            return;
        }
        float degrees = Mth.lerp(random.nextFloat(), NUDGE_MIN_DEGREES, NUDGE_MAX_DEGREES);
        placement.moveTo(placement.yaw + (random.nextBoolean() ? degrees : -degrees),
                Mth.clamp(placement.height + (random.nextFloat() * 2 - 1) * NUDGE_HEIGHT, MIN_HEIGHT, MAX_HEIGHT));
    }

    private @Nullable Pick pick(LocalPlayer player, float partialTick, boolean isThroughBlocks) {
        Vec3 from = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        Pick best = null;
        for (RequestsS2CPacket.Entry entry : ClientRequestSystem.INSTANCE.get(player.getId())) {
            MagicCircle circle = getCircle(player, entry, partialTick);
            double distance = circle == null ? -1 : circle.getHitDistance(from, look);
            if (distance >= 0 && (best == null || distance < best.distance()) && (isThroughBlocks || !Occlusion.isBlocked(from, look, distance))) {
                best = new Pick(Key.of(entry), distance);
            }
        }
        return best;
    }

    public @Nullable AnyPick pickAny(Vec3 from, Vec3 look) {
        if (level == null) {
            return null;
        }
        Entity self = Minecraft.getInstance().player;
        AnyPick best = null;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity.isSpectator() && entity != self || ClientHidingSystem.INSTANCE.isEntityHidden(entity.getId())) {
                continue;
            }
            for (RequestsS2CPacket.Entry entry : ClientRequestSystem.INSTANCE.get(entity.getId())) {
                MagicCircle circle = getCircle(entity, entry, 1);
                double distance = circle == null ? -1 : circle.getHitDistance(from, look);
                if (circle != null && distance >= 0 && (best == null || distance < best.distance()) && !Occlusion.isBlocked(from, look, distance)) {
                    best = new AnyPick(entity.getId(), entry, circle.center(), distance);
                }
            }
        }
        return best;
    }

    public @Nullable Vec3 getCenter(int entityId, UUID id, int role) {
        Entity host = level == null ? null : level.getEntity(entityId);
        if (host == null) {
            return null;
        }
        for (RequestsS2CPacket.Entry entry : ClientRequestSystem.INSTANCE.get(entityId)) {
            if (entry.id().equals(id) && entry.role() == role) {
                MagicCircle circle = getCircle(host, entry, 1);
                return circle == null ? null : circle.center();
            }
        }
        return null;
    }

    public record AnyPick(int entityId, RequestsS2CPacket.Entry entry, Vec3 center, double distance) {
    }

    public void collectCircles(float partialTick, List<MagicCircleRenderer.CircleDraw> out) {
        if (level == null) {
            return;
        }
        Entity self = Minecraft.getInstance().player;
        for (Entity entity : level.entitiesForRendering()) {
            List<RequestsS2CPacket.Entry> entries = ClientRequestSystem.INSTANCE.get(entity.getId());
            if (entries.isEmpty() || entity.isSpectator() && entity != self || ClientHidingSystem.INSTANCE.isEntityHidden(entity.getId())) {
                continue;
            }
            float alpha = ClientHidingSystem.INSTANCE.getEntity(entity.getId());
            for (RequestsS2CPacket.Entry entry : entries) {
                MagicCircle circle = getCircle(entity, entry, partialTick);
                if (circle != null) {
                    MagicCircleRenderer.CircleDraw draw = toDraw(entity == self ? placements.get(Key.of(entry)) : null, entry, circle, partialTick);
                    if (entity == self) {
                        draw = draw.throughBlocks();
                    }
                    draw = draw.faded(alpha);
                    out.add(draw);
                    if (entry.isWorld()) {
                        out.add(getWorldRing(draw));
                    }
                }
            }
        }
    }

    private static MagicCircleRenderer.CircleDraw getWorldRing(MagicCircleRenderer.CircleDraw draw) {
        MagicCircle circle = draw.circle();
        MagicCircle ring = new MagicCircle(circle.center(), circle.normal(), circle.radius() * WORLD_RING_SCALE);
        return new MagicCircleRenderer.CircleDraw(ring, (draw.seed() + WORLD_RING_SEED) & MagicCircle.SEED_MASK, -draw.time(), draw.fade(), draw.brightness(), Palette.of(WORLD_RING_TINT),
                "", draw.charge(), "");
    }

    private MagicCircleRenderer.CircleDraw toDraw(@Nullable Placement placement, RequestsS2CPacket.Entry entry, MagicCircle circle, float partialTick) {
        float time = age + partialTick;
        Key key = Key.of(entry);
        float fade = placement == null ? 1 : Mth.clamp((time - placement.appearedAt) / APPEAR_TICKS, 0, 1);
        float brightness;
        if (entry.isRead()) {
            fade *= READ_FADE;
            brightness = READ_BRIGHTNESS;
        } else {
            float shine = Math.max(0, 1 - (time % SHINE_INTERVAL_TICKS) / SHINE_TICKS);
            brightness = UNREAD_BRIGHTNESS + UNREAD_PULSE * Mth.sin(time * PULSE_SPEED) + SHINE * shine;
        }
        float build = 1;
        if (key.equals(charging)) {
            float remaining = 1 - Mth.lerp(partialTick, prevCharge, charge);
            build = 1 - remaining * remaining * remaining;
            brightness += 0.3f * (1 - remaining);
        }
        String text = I18n.get(entry.key(), entry.args().toArray()).toLowerCase();
        return new MagicCircleRenderer.CircleDraw(circle, entry.id().hashCode() & MagicCircle.SEED_MASK, time, fade, brightness, Palette.of(entry.color()), text, build, "");
    }

    public @Nullable MagicCircle getCircle(Entity host, RequestsS2CPacket.Entry entry, float partialTick) {
        Key key = Key.of(entry);
        Placement placement = host == Minecraft.getInstance().player ? placements.get(key) : null;
        float yaw;
        float height;
        float openness = 0;
        if (placement != null) {
            yaw = placement.getShownYaw(partialTick);
            height = placement.getShownHeight(partialTick);
            openness = Mth.lerp(partialTick, placement.prevOpenness, placement.openness);
        } else {
            yaw = (entry.id().hashCode() & 0xFFFF) / (float) 0xFFFF * 360;
            height = REST_HEIGHT;
            openness = entry.isOpen() ? 1 : 0;
        }
        float time = age + partialTick;
        float bob = (entry.isRead() ? READ_BOB : UNREAD_BOB) * Mth.sin(time * BOB_SPEED + (entry.id().hashCode() & MagicCircle.SEED_MASK));
        Vec3 eye = host.getEyePosition(partialTick);
        float radians = yaw * Mth.DEG_TO_RAD;
        Vec3 center = eye.add(-Mth.sin(radians) * ORBIT, height + bob - OPEN_DROP * openness, Mth.cos(radians) * ORBIT);
        Vec3 facing = eye.subtract(center).normalize();
        Vec3 normal = facing.lerp(MagicCircle.UP, openness).normalize();
        float radius = entry.isRead() ? READ_RADIUS : UNREAD_RADIUS;
        return new MagicCircle(CircleClearance.lift(key, center, normal, radius, true), normal, radius);
    }

    private record Key(UUID id, int role) {
        private static Key of(RequestsS2CPacket.Entry entry) {
            return new Key(entry.id(), entry.role());
        }

        private boolean matches(RequestsS2CPacket.Entry entry) {
            return id.equals(entry.id()) && role == entry.role();
        }
    }

    private record Pick(Key key, double distance) {
    }

    private static class Placement extends CirclePlacement {
        private final int appearedAt;
        private float openness;
        private float prevOpenness;

        private Placement(float yaw, float height, int appearedAt) {
            super(yaw, height, PLACE_EASE);
            this.appearedAt = appearedAt;
        }

        private void tick(boolean isOpen) {
            tick();
            prevOpenness = openness;
            openness = Mth.lerp(OPEN_EASE, openness, isOpen ? 1 : 0);
        }
    }
}
