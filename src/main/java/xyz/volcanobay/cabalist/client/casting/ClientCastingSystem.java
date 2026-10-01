package xyz.volcanobay.cabalist.client.casting;

import com.mojang.blaze3d.platform.InputConstants;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.network.VeilPacketManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircleRenderer;
import xyz.volcanobay.cabalist.client.renderer.circle.Occlusion;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphRenderer;
import xyz.volcanobay.cabalist.client.request.DraftCircles;
import xyz.volcanobay.cabalist.client.request.NotificationCircles;
import xyz.volcanobay.cabalist.client.spell.ClientHangingSpellSystem;
import xyz.volcanobay.cabalist.client.spell.ClientSpellVisualSystem;
import xyz.volcanobay.cabalist.client.visibility.ClientHidingSystem;
import xyz.volcanobay.cabalist.networking.packet.*;
import xyz.volcanobay.cabalist.system.casting.CastingEdit;
import xyz.volcanobay.cabalist.system.casting.CastingPhase;
import xyz.volcanobay.cabalist.system.casting.CastingSession;
import xyz.volcanobay.cabalist.system.focus.Focus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Holds the local caster's input, and everyone's casting scenes,
 */
public class ClientCastingSystem {
    public static final ClientCastingSystem INSTANCE = new ClientCastingSystem();

    private static final double DEGREES_PER_TURN = 0.15;
    private static final double WRITING_TURN_SCALE = 0.5;
    private static final double RECENTER_PER_SECOND = 4;
    private static final double MAX_OFFSET_DEGREES = 30;
    private static final int MAX_ANSWER = 24;
    private static final double DISMISS_RANGE = 24;
    private static final double TEAR_RADIUS = 0.6;
    private static final UUID NO_ID = new UUID(0, 0);

    private final Int2ObjectMap<CastingScene> scenes = new Int2ObjectOpenHashMap<>();
    private final List<Edit> pending = new ArrayList<>();
    private List<String> serverWords = List.of();
    private String serverCurrent = "";
    private int nextSeq;
    private boolean wasUseDown;
    private boolean isHolding;
    private @Nullable LookTargetC2SPacket dismissing;
    private LookTargetC2SPacket lookTarget = LookTargetC2SPacket.NOTHING;
    private String answer = "";
    private String judgedAnswer = "";
    private int verdict = CastingSession.ANSWER_NONE;
    private boolean isSubmitting;
    private double offsetYaw;
    private double offsetPitch;
    private long lastFrame = Util.getMillis();

    public static boolean isFocus(ItemStack stack) {
        return Focus.canHold(stack) && Focus.get(stack).getTotalLifeforce() > 0;
    }

    public void onState(CastingStateS2CPacket packet) {
        CastingScene scene = scenes.get(packet.entityId());
        if (scene == null || scene.isEnding()) {
            scene = new CastingScene(packet.entityId());
            scenes.put(packet.entityId(), scene);
        }
        CastingPhase phase = CastingPhase.byId(packet.phase());
        boolean startsWriting = !canWrite(scene.getPhase()) && canWrite(phase);
        scene.setStatus(phase, packet.palette(), packet.gathered(), packet.capacity(), packet.rate(), packet.frozen(), packet.charge(), packet.aim(), packet.spoken());
        scene.setDismiss(packet.isDismissing(), packet.dismissAt(), packet.dismiss());
        scene.setPush(packet.isPushing(), packet.pushAt());
        if (isLocal(packet.entityId())) {
            if (startsWriting) {
                releaseKeysExceptUse();
            }
            serverWords = packet.words();
            serverCurrent = packet.current();
            pending.removeIf(edit -> edit.seq <= packet.ackSeq());
            refreshLocalText(scene);
            judgedAnswer = packet.answer();
            verdict = packet.verdict();
            scene.setAnswerValid(isJudged() && verdict != CastingSession.ANSWER_NONE);
            if (isSubmitting && isJudged()) {
                isSubmitting = false;
                submit(scene);
            }
        } else {
            scene.setText(packet.words(), packet.current());
        }
    }

    public void onEnd(CastingEndS2CPacket packet) {
        CastingScene scene = scenes.get(packet.entityId());
        if (scene != null) {
            scene.end(packet.cast());
        }
        if (isLocal(packet.entityId())) {
            pending.clear();
            answer = "";
            serverWords = List.of();
            serverCurrent = "";
        }
    }

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            scenes.clear();
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        for (Iterator<CastingScene> iterator = scenes.values().iterator(); iterator.hasNext(); ) {
            CastingScene scene = iterator.next();
            CastingBasis basis = getBasis(scene, camera, 1);
            scene.tick(basis, getTearBox(scene, basis), getPushPoint(scene, basis));
            if (scene.isFinished()) {
                iterator.remove();
            }
        }
        tickInput(minecraft);
    }

    private void tickInput(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            return;
        }
        boolean isDown = minecraft.options.keyUse.isDown();
        CastingScene scene = getActiveLocalScene();
        if (isDown && !wasUseDown && (scene == null ? isFocus(player.getMainHandItem()) : scene.getPhase() == CastingPhase.WRITING)) {
            send(CastingInputC2SPacket.HOLD, 0, "");
            isHolding = true;
        } else if (!isDown && isHolding) {
            send(CastingInputC2SPacket.RELEASE, 0, "");
            isHolding = false;
        }
        wasUseDown = isDown;
        tickDismissal(minecraft, player, scene);
        tickLookTarget(player, scene);
    }

    // Tells the server which circle is under the crosshair, so "this" can mean it.
    private void tickLookTarget(LocalPlayer player, @Nullable CastingScene scene) {
        LookTargetC2SPacket target = scene == null ? LookTargetC2SPacket.NOTHING : getLookTargetPacket(player, false);
        if (!target.isSameTarget(lookTarget)) {
            lookTarget = target;
            VeilPacketManager.server().sendPacket(target);
        }
    }

    // The nearest circle or form under the crosshair. Dismissing skips other players' notifications because that would be broken.
    private static LookTargetC2SPacket getLookTargetPacket(LocalPlayer player, boolean isDismissing) {
        Vec3 from = player.getEyePosition();
        Vec3 look = player.getViewVector(1);
        LookTargetC2SPacket best = LookTargetC2SPacket.NOTHING;
        NotificationCircles.AnyPick request = NotificationCircles.INSTANCE.pickAny(from, look);
        if (request != null && (!isDismissing || request.entityId() == player.getId())) {
            best = nearer(best, new LookTargetC2SPacket(LookTargetC2SPacket.REQUEST, request.entityId(), request.entry().id(), request.entry().role(), 0, 0,
                    request.center(), (float) request.distance()));
        }
        ClientHangingSpellSystem.CirclePick spell = ClientHangingSpellSystem.INSTANCE.pick(from, look, DISMISS_RANGE, 1);
        if (spell != null) {
            Vec3 center = ClientHangingSpellSystem.INSTANCE.getCircleCenter(spell.entityId(), spell.index(), 1);
            best = nearer(best, new LookTargetC2SPacket(LookTargetC2SPacket.SPELL, spell.entityId(), NO_ID, 0, spell.index(), spell.wordsHash(),
                    center == null ? from : center, (float) spell.distance()));
        }
        ClientSpellVisualSystem.FormPick form = ClientSpellVisualSystem.INSTANCE.pick(from, look, DISMISS_RANGE);
        if (form != null) {
            best = nearer(best, new LookTargetC2SPacket(LookTargetC2SPacket.FORM, -1, NO_ID, 0, form.visualId(), 0, form.point(), (float) form.distance()));
        }
        DraftCircles.DraftPick draft = DraftCircles.INSTANCE.pick(from, look);
        if (draft != null) {
            best = nearer(best, new LookTargetC2SPacket(LookTargetC2SPacket.DRAFT, -1, draft.draft(), 0, draft.circle(), 0, draft.center(), (float) draft.distance()));
        }
        return best;
    }

    private static LookTargetC2SPacket nearer(LookTargetC2SPacket best, LookTargetC2SPacket other) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && Occlusion.isBlocked(player.getEyePosition(), player.getViewVector(1), other.distance())) {
            return best;
        }
        return best.kind() == LookTargetC2SPacket.NONE || other.distance() < best.distance() ? other : best;
    }

    // Where a picked target is now, or null once it's gone.
    private static @Nullable Vec3 getTargetCenter(LookTargetC2SPacket target) {
        return switch (target.kind()) {
            case LookTargetC2SPacket.SPELL ->
                    ClientHangingSpellSystem.INSTANCE.getCircleCenter(target.hostId(), target.index(), 1);
            case LookTargetC2SPacket.FORM -> ClientSpellVisualSystem.INSTANCE.getCenter(target.index());
            case LookTargetC2SPacket.REQUEST ->
                    NotificationCircles.INSTANCE.getCenter(target.hostId(), target.requestId(), target.role());
            case LookTargetC2SPacket.DRAFT -> DraftCircles.INSTANCE.getCenter(target.requestId(), target.index());
            default -> null;
        };
    }

    /**
     * Holding left click locks onto what's under the crosshair until it's let go or the target is gone. This "casts" dispell on the target.
     */
    private void tickDismissal(Minecraft minecraft, LocalPlayer player, @Nullable CastingScene scene) {
        boolean isAttackDown = minecraft.options.keyAttack.isDown() && scene != null && scene.getPhase() != CastingPhase.CHARGING;
        if (dismissing != null && (!isAttackDown || getTargetCenter(dismissing) == null)) {
            dismissing = null;
            VeilPacketManager.server().sendPacket(CastingDismissC2SPacket.STOP);
        }
        if (isAttackDown && dismissing == null) {
            LookTargetC2SPacket target = getLookTargetPacket(player, true);
            if (target.kind() != LookTargetC2SPacket.NONE) {
                dismissing = target;
                VeilPacketManager.server().sendPacket(new CastingDismissC2SPacket(target));
            }
        }
    }

    public boolean isCasting() {
        return getActiveLocalScene() != null;
    }

    /**
     * Where glyphs are torn off what's being dismissed, relative to the caster's eyes. The caster follows it live.
     */
    private @Nullable AABB getTearBox(CastingScene scene, @Nullable CastingBasis basis) {
        if (basis == null || !scene.isDismissing()) {
            return null;
        }
        Vec3 live = isLocal(scene.getEntityId()) && dismissing != null ? getTargetCenter(dismissing) : null;
        Vec3 center = live != null ? live : scene.getDismissAt();
        return new AABB(center, center).inflate(TEAR_RADIUS).move(-basis.origin().x, -basis.origin().y, -basis.origin().z);
    }

    private @Nullable Vec3 getPushPoint(CastingScene scene, @Nullable CastingBasis basis) {
        if (basis == null || !scene.isPushing()) {
            return null;
        }
        Vec3 live = isLocal(scene.getEntityId()) ? getTargetCenter(lookTarget) : null;
        return (live != null ? live : scene.getPushAt()).subtract(basis.origin());
    }

    public boolean onKey(long window, int key, int action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (window != minecraft.getWindow().getWindow() || minecraft.screen != null || !isLocalWriting()) {
            return false;
        }
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25 || isHandingKeysBack(window, key, action)) {
            return false;
        }
        if (action == GLFW.GLFW_RELEASE) {
            return true;
        }
        CastingScene scene = getActiveLocalScene();
        if (scene != null && scene.isPrompting()) {
            onPromptKey(scene, key);
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                if (scene != null && scene.isEmpty()) {
                    send(CastingInputC2SPacket.ABANDON, 0, "");
                } else if (scene != null) {
                    setAnswer(scene, "");
                    scene.showPrompt(I18n.get("casting.cabalist.abandon_spell"));
                }
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> send(CastingInputC2SPacket.CAST, 0, "");
            case GLFW.GLFW_KEY_BACKSPACE -> edit(CastingEdit.BACKSPACE, "");
            default -> {
            }
        }
        return true;
    }

    // Enter with anything other than yes or no asks again.
    private void onPromptKey(CastingScene scene, int key) {
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> closePrompt(scene);
            case GLFW.GLFW_KEY_BACKSPACE ->
                    setAnswer(scene, answer.isEmpty() ? "" : answer.substring(0, answer.length() - 1));
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (isJudged()) {
                    submit(scene);
                } else {
                    isSubmitting = true;
                }
            }
            default -> {
            }
        }
    }

    /**
     * The server judges answers with the spell dictionary, so Enter waits for the latest verdict.
     */
    private boolean isJudged() {
        return judgedAnswer.equals(answer);
    }

    private void submit(CastingScene scene) {
        if (verdict == CastingSession.ANSWER_YES) {
            send(CastingInputC2SPacket.ABANDON, 0, "");
        } else if (verdict == CastingSession.ANSWER_NO) {
            closePrompt(scene);
        } else {
            setAnswer(scene, "");
            scene.showPrompt(I18n.get("casting.cabalist.yes_or_no"));
        }
    }

    private void setAnswer(CastingScene scene, String text) {
        answer = text;
        scene.setAnswer(text);
        scene.setAnswerValid(isJudged() && verdict != CastingSession.ANSWER_NONE);
        send(CastingInputC2SPacket.PROMPT_ANSWER, 0, text);
    }

    private void closePrompt(CastingScene scene) {
        setAnswer(scene, "");
        isSubmitting = false;
        scene.closePrompt();
    }

    // Holding ctrl hands the keys back to the game so the caster can move with the spell still up.
    // also changing this key will break things, as this doesn't consume the action, so please don't.
    public static boolean isHandingKeysBack(long window, int key, int action) {
        boolean isCtrl = key == GLFW.GLFW_KEY_LEFT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_CONTROL;
        if (isCtrl && action == GLFW.GLFW_RELEASE) {
            releaseKeysExceptUse();
            return true;
        }
        return isCtrl || isControlHeld(window);
    }

    public static boolean isControlHeld(long window) {
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    public boolean onChar(long window, int codePoint) {
        Minecraft minecraft = Minecraft.getInstance();
        if (window != minecraft.getWindow().getWindow() || minecraft.screen != null || !isLocalWriting() || isControlHeld(window)) {
            return false;
        }
        char character = Character.toLowerCase((char) codePoint);
        CastingScene scene = getActiveLocalScene();
        if (scene != null && scene.isPrompting()) {
            if ((Character.isLetter(character) || character == ' ') && answer.length() < MAX_ANSWER) {
                setAnswer(scene, answer + character);
            }
            return true;
        }
        if (character == ' ') {
            edit(CastingEdit.SPACE, "");
        } else if (CastingEdit.isTypeable(character)) {
            edit(CastingEdit.TYPE, String.valueOf(character));
        }
        return true;
    }

    // Turning is slowed while casting; the glyphs move by the full turn and drift back in front.
    public double onTurn(double yaw, double pitch) {
        double scale = getTurnScale();
        if (scale < 1) {
            offsetYaw = Mth.clamp(offsetYaw - yaw * DEGREES_PER_TURN * (1 - scale), -MAX_OFFSET_DEGREES, MAX_OFFSET_DEGREES);
            offsetPitch = Mth.clamp(offsetPitch - pitch * DEGREES_PER_TURN * (1 - scale), -MAX_OFFSET_DEGREES, MAX_OFFSET_DEGREES);
        }
        return scale;
    }

    // Aiming lets the camera turn normally again as the cast gets ready.
    private double getTurnScale() {
        CastingScene scene = getActiveLocalScene();
        if (scene == null) {
            return 1;
        }
        if (scene.getPhase() == CastingPhase.AIMING) {
            return Mth.lerp(scene.getAim(), WRITING_TURN_SCALE, 1);
        }
        return WRITING_TURN_SCALE;
    }

    public void render(MatrixStack pose, Camera camera, float partialTick) {
        long now = Util.getMillis();
        double decay = Math.exp(-(now - lastFrame) / 1000.0 * RECENTER_PER_SECOND);
        lastFrame = now;
        offsetYaw *= decay;
        offsetPitch *= decay;
        if (scenes.isEmpty()) {
            return;
        }
        Vec3 cameraRight = new Vec3(camera.getLeftVector()).scale(-1);
        Vec3 cameraUp = new Vec3(camera.getUpVector());
        GlyphRenderer glyphs = GlyphRenderer.INSTANCE.begin(GlyphRenderer.Depth.TESTED);
        for (CastingScene scene : scenes.values()) {
            if (isHidden(scene)) {
                continue;
            }
            CastingBasis basis = getBasis(scene, camera, partialTick);
            if (basis != null) {
                Vec3 origin = basis.origin().subtract(camera.getPosition());
                scene.render(glyphs, pose, origin, basis, cameraRight, cameraUp, false, partialTick);
                if (!isOwnView(scene, camera)) {
                    scene.render(glyphs, pose, origin, basis, cameraRight, cameraUp, true, partialTick);
                }
            }
        }
        glyphs.end();
        // The caster's own write head is drawn over everything so walls in front of them don't cut through it.
        glyphs = GlyphRenderer.INSTANCE.begin(GlyphRenderer.Depth.ALWAYS);
        for (CastingScene scene : scenes.values()) {
            if (isHidden(scene)) {
                continue;
            }
            CastingBasis basis = getBasis(scene, camera, partialTick);
            if (basis != null && isOwnView(scene, camera)) {
                scene.render(glyphs, pose, basis.origin().subtract(camera.getPosition()), basis, cameraRight, cameraUp, true, partialTick);
            }
        }
        glyphs.end();
    }

    public void collectCircles(Camera camera, float partialTick, List<MagicCircleRenderer.CircleDraw> out) {
        for (CastingScene scene : scenes.values()) {
            if (isHidden(scene)) {
                continue;
            }
            CastingBasis basis = getBasis(scene, camera, partialTick);
            if (basis != null) {
                int before = out.size();
                scene.collectCircle(basis.origin(), basis, partialTick, out);
                if (isOwnView(scene, camera) && out.size() > before) {
                    out.set(before, out.get(before).throughBlocks());
                }
            }
        }
    }

    private @Nullable CastingBasis getBasis(CastingScene scene, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(scene.getEntityId());
        if (entity == null) {
            return scene.getView();
        }
        if (isOwnView(scene, camera)) {
            Vec3 forward = Vec3.directionFromRotation((float) (camera.getXRot() + offsetPitch), (float) (camera.getYRot() + offsetYaw));
            return CastingBasis.of(camera.getPosition(), forward);
        }
        return CastingBasis.of(entity.getEyePosition(partialTick), entity.getViewVector(partialTick));
    }

    private static boolean isHidden(CastingScene scene) {
        return ClientHidingSystem.INSTANCE.isEntityHidden(scene.getEntityId());
    }

    private boolean isOwnView(CastingScene scene, Camera camera) {
        return !camera.isDetached() && camera.getEntity() != null && camera.getEntity().getId() == scene.getEntityId();
    }

    private void edit(int edit, String text) {
        int seq = ++nextSeq;
        pending.add(new Edit(seq, edit, text));
        send(edit, seq, text);
        CastingScene scene = scenes.get(getLocalId());
        if (scene != null) {
            refreshLocalText(scene);
        }
    }

    private void refreshLocalText(CastingScene scene) {
        List<String> words = new ArrayList<>(serverWords);
        String current = serverCurrent;
        for (Edit edit : pending) {
            current = CastingEdit.apply(edit.edit, edit.text, words, current);
        }
        scene.setText(words, current);
    }

    private boolean isLocalWriting() {
        CastingScene scene = getActiveLocalScene();
        return scene != null && canWrite(scene.getPhase());
    }

    private static boolean canWrite(CastingPhase phase) {
        return phase.isWriting() || phase == CastingPhase.CHARGED;
    }

    public static void releaseKeysExceptUse() {
        Minecraft minecraft = Minecraft.getInstance();
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if (mapping != minecraft.options.keyUse) {
                mapping.setDown(false);
            }
        }
    }

    private @Nullable CastingScene getActiveLocalScene() {
        CastingScene scene = scenes.get(getLocalId());
        return scene == null || scene.isEnding() ? null : scene;
    }

    private static int getLocalId() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? -1 : player.getId();
    }

    private static boolean isLocal(int entityId) {
        return entityId == getLocalId();
    }

    private static void send(int action, int seq, String text) {
        VeilPacketManager.server().sendPacket(new CastingInputC2SPacket(action, seq, text));
    }

    private record Edit(int seq, int edit, String text) {
    }
}
