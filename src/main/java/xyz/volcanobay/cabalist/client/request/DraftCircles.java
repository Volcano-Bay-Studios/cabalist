package xyz.volcanobay.cabalist.client.request;

import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.client.renderer.circle.CircleClearance;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircle;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircleRenderer;
import xyz.volcanobay.cabalist.client.renderer.circle.Occlusion;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphRenderer;
import xyz.volcanobay.cabalist.client.visibility.ClientHidingSystem;
import xyz.volcanobay.cabalist.networking.packet.DraftsS2CPacket;
import xyz.volcanobay.cabalist.system.render.Palette;
import xyz.volcanobay.cabalist.util.ColorHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// Draft circles ring the entity or block holding the contract, facing out, each always showing its text. Only the author can drag them.
public class DraftCircles {
    public static final DraftCircles INSTANCE = new DraftCircles();

    private static final float RADIUS = 0.4f;
    private static final float GAP = 0.35f;
    private static final float MIN_ORBIT = 1.4f;
    private static final float REST_HEIGHT = 0.3f;
    private static final float MIN_HEIGHT = -1;
    private static final float MAX_HEIGHT = 1.5f;
    private static final float BOB = 0.04f;
    private static final float BOB_SPEED = 0.05f;
    private static final float BRIGHTNESS = 0.7f;
    private static final float PLACE_EASE = 0.2f;
    private static final int WRAP = 24;

    private final Map<CircleKey, CirclePlacement> placements = new HashMap<>();
    private final Map<CircleKey, ReadingScene> scenes = new HashMap<>();
    private List<DraftsS2CPacket.Draft> drafts = List.of();
    private @Nullable ClientLevel level;
    private @Nullable CircleKey dragging;
    private int age;

    private record CircleKey(UUID draft, int circle) {
    }

    public record DraftPick(UUID draft, int circle, Vec3 center, double distance) {
    }

    public void set(List<DraftsS2CPacket.Draft> newDrafts) {
        checkLevel();
        drafts = List.copyOf(newDrafts);
    }

    private void checkLevel() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != level) {
            level = current;
            drafts = List.of();
            placements.clear();
            scenes.clear();
            dragging = null;
        }
    }

    public void tick() {
        checkLevel();
        if (level == null) {
            return;
        }
        age++;
        Set<CircleKey> present = new HashSet<>();
        for (DraftsS2CPacket.Draft draft : drafts) {
            List<DraftsS2CPacket.Circle> circles = draft.circles();
            for (int i = 0; i < circles.size(); i++) {
                DraftsS2CPacket.Circle circle = circles.get(i);
                CircleKey key = new CircleKey(draft.id(), circle.id());
                present.add(key);
                float defaultYaw = 360f * i / circles.size();
                placements.computeIfAbsent(key, k -> new CirclePlacement(defaultYaw, REST_HEIGHT, PLACE_EASE)).tick();
                ReadingScene scene = scenes.computeIfAbsent(key, k -> new ReadingScene());
                scene.setLines(getLines(circle));
                scene.tick(circle.isLatin());
            }
        }
        placements.keySet().retainAll(present);
        scenes.keySet().retainAll(present);
        tickDrag();
    }

    private static List<ReadingScene.Line> getLines(DraftsS2CPacket.Circle circle) {
        String text = circle.kind() == DraftsS2CPacket.KIND_ADD ? I18n.get("request.cabalist.draft_add") : circle.text();
        List<ReadingScene.Line> lines = new ArrayList<>();
        for (String line : RequestReading.wrap(text.toLowerCase(Locale.ROOT), WRAP)) {
            lines.add(new ReadingScene.Line(line, ColorHelper.lighten(circle.color())));
        }
        return lines;
    }

    // Dragging swings the circle to where the crosshair points around the holder.
    private void tickDrag() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        CirclePlacement placement = dragging == null ? null : placements.get(dragging);
        if (player == null || placement == null || !minecraft.options.keyUse.isDown()) {
            dragging = null;
            return;
        }
        DraftsS2CPacket.Draft draft = find(dragging.draft());
        Vec3 anchor = draft == null ? null : getAnchor(draft, 1);
        if (anchor == null) {
            dragging = null;
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 point = eye.add(player.getViewVector(1).scale(eye.distanceTo(anchor)));
        placement.moveTo((float) (Mth.atan2(-(point.x - anchor.x), point.z - anchor.z) * Mth.RAD_TO_DEG),
                Mth.clamp((float) (point.y - anchor.y), MIN_HEIGHT, MAX_HEIGHT));
    }

    public boolean isDragging() {
        return dragging != null;
    }

    public boolean tryStartDrag(LocalPlayer player) {
        DraftPick pick = pick(player.getEyePosition(), player.getViewVector(1));
        DraftsS2CPacket.Draft draft = pick == null ? null : find(pick.draft());
        if (draft == null || !draft.author().equals(player.getUUID())) {
            return false;
        }
        dragging = new CircleKey(pick.draft(), pick.circle());
        return true;
    }

    private @Nullable DraftsS2CPacket.Draft find(UUID id) {
        for (DraftsS2CPacket.Draft draft : drafts) {
            if (draft.id().equals(id)) {
                return draft;
            }
        }
        return null;
    }

    // A draft held by something hidden is hidden with it.
    private @Nullable Vec3 getAnchor(DraftsS2CPacket.Draft draft, float partialTick) {
        if (draft.anchorBlock().isPresent()) {
            BlockPos pos = draft.anchorBlock().get();
            return ClientHidingSystem.isHidden(ClientHidingSystem.INSTANCE.getBlock(pos)) ? null : pos.getCenter();
        }
        Entity entity = level == null ? null : level.getEntity(draft.anchorEntity());
        if (entity == null || ClientHidingSystem.INSTANCE.isEntityHidden(entity.getId())) {
            return null;
        }
        return entity.getPosition(partialTick).add(0, entity.getBbHeight() * 0.6, 0);
    }

    private @Nullable MagicCircle getCircle(DraftsS2CPacket.Draft draft, int index, float partialTick) {
        Vec3 anchor = getAnchor(draft, partialTick);
        DraftsS2CPacket.Circle circle = draft.circles().get(index);
        CirclePlacement placement = placements.get(new CircleKey(draft.id(), circle.id()));
        if (anchor == null || placement == null) {
            return null;
        }
        int count = draft.circles().size();
        float orbit = Math.max(MIN_ORBIT, count * (2 * RADIUS + GAP) / Mth.TWO_PI);
        float yaw = placement.getShownYaw(partialTick) * Mth.DEG_TO_RAD;
        float height = placement.getShownHeight(partialTick);
        float bob = BOB * Mth.sin((age + partialTick) * BOB_SPEED + index);
        Vec3 outward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 center = CircleClearance.lift(placement, anchor.add(outward.scale(orbit)).add(0, height + bob, 0), outward, RADIUS, false);
        return new MagicCircle(center, outward, RADIUS);
    }

    public @Nullable Vec3 getCenter(UUID draftId, int circleId) {
        DraftsS2CPacket.Draft draft = find(draftId);
        if (draft == null) {
            return null;
        }
        for (int i = 0; i < draft.circles().size(); i++) {
            if (draft.circles().get(i).id() == circleId) {
                MagicCircle circle = getCircle(draft, i, 1);
                return circle == null ? null : circle.center();
            }
        }
        return null;
    }

    public @Nullable DraftPick pick(Vec3 from, Vec3 look) {
        DraftPick best = null;
        for (DraftsS2CPacket.Draft draft : drafts) {
            for (int i = 0; i < draft.circles().size(); i++) {
                MagicCircle circle = getCircle(draft, i, 1);
                if (circle == null) {
                    continue;
                }
                double distance = circle.getHitDistance(from, look);
                if (distance >= 0 && (best == null || distance < best.distance()) && !Occlusion.isBlocked(from, look, distance)) {
                    best = new DraftPick(draft.id(), draft.circles().get(i).id(), circle.center(), distance);
                }
            }
        }
        return best;
    }

    public void collectCircles(float partialTick, List<MagicCircleRenderer.CircleDraw> out) {
        for (DraftsS2CPacket.Draft draft : drafts) {
            for (int i = 0; i < draft.circles().size(); i++) {
                MagicCircle circle = getCircle(draft, i, partialTick);
                DraftsS2CPacket.Circle info = draft.circles().get(i);
                if (circle != null) {
                    out.add(new MagicCircleRenderer.CircleDraw(circle, (draft.id().hashCode() * 31 + info.id()) & MagicCircle.SEED_MASK, age + partialTick, 1, BRIGHTNESS,
                            Palette.of(info.color()), "", 1, ""));
                }
            }
        }
    }

    public void render(MatrixStack pose, Camera camera, float partialTick) {
        if (drafts.isEmpty()) {
            return;
        }
        Vec3 right = new Vec3(camera.getLeftVector()).scale(-1);
        Vec3 up = new Vec3(camera.getUpVector());
        GlyphRenderer glyphs = GlyphRenderer.INSTANCE.begin(GlyphRenderer.Depth.TESTED);
        for (DraftsS2CPacket.Draft draft : drafts) {
            for (int i = 0; i < draft.circles().size(); i++) {
                MagicCircle circle = getCircle(draft, i, partialTick);
                ReadingScene scene = scenes.get(new CircleKey(draft.id(), draft.circles().get(i).id()));
                if (circle != null && scene != null) {
                    scene.render(glyphs, pose, circle.center().subtract(camera.getPosition()), right, up, partialTick, 1);
                }
            }
        }
        glyphs.end();
    }
}
