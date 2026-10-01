package xyz.volcanobay.cabalist.client.request;

import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import xyz.volcanobay.cabalist.client.casting.ClientCastingSystem;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircle;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphRenderer;
import xyz.volcanobay.cabalist.client.visibility.ClientHidingSystem;
import xyz.volcanobay.cabalist.networking.packet.RequestAnswerC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.RequestVerdictS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.RequestsS2CPacket;
import xyz.volcanobay.cabalist.system.request.PartyStatus;
import xyz.volcanobay.cabalist.system.request.Request;
import xyz.volcanobay.cabalist.util.ColorHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// Open request circles and the answer the local player is typing into theirs.
public class RequestReading {
    public static final RequestReading INSTANCE = new RequestReading();

    private static final int WRAP = 32;
    private static final int MAX_ANSWER = 32;
    private static final float ANSWER_Y = 0.24f;
    private static final float HINT_Y = 0.13f;
    private static final int VALID_ANSWER = 0x8CFF7A;
    private static final int INVALID_ANSWER = 0x9A9A9A;
    private static final int HINT = 0xC8C8C8;
    private static final float THROUGH_BLOCKS_ALPHA = 0.3f;
    private static final int ADDED = 0x8CFF7A;
    private static final int REMOVED = 0xFF7A6A;
    private static final int EDITED = 0xFFD34D;

    private final Map<SceneKey, Open> scenes = new HashMap<>();
    private @Nullable UUID answering;
    private String answer = "";
    private String judgedAnswer = "";
    private PartyStatus verdict = PartyStatus.UNANSWERED;
    private boolean isHinting;
    private TypedLine answerLine = new TypedLine();
    private TypedLine hintLine = new TypedLine();

    public void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            scenes.clear();
            return;
        }
        for (Open open : scenes.values()) {
            open.isSeen = false;
        }
        Entity self = Minecraft.getInstance().player;
        for (Entity entity : level.entitiesForRendering()) {
            if (ClientHidingSystem.INSTANCE.isEntityHidden(entity.getId())) {
                continue;
            }
            for (RequestsS2CPacket.Entry entry : ClientRequestSystem.INSTANCE.get(entity.getId())) {
                if (!NotificationCircles.INSTANCE.isOpen(entity, entry)) {
                    continue;
                }
                Open open = scenes.computeIfAbsent(new SceneKey(entity.getId(), entry.id(), entry.role()), key -> new Open());
                open.entity = entity;
                open.entry = entry;
                open.isSeen = true;
                open.scene.setLines(getLines(entry));
            }
        }
        for (Iterator<Open> iterator = scenes.values().iterator(); iterator.hasNext(); ) {
            Open open = iterator.next();
            if (!open.isSeen) {
                open.scene.close();
            }
            open.scene.tick(open.entity == self);
            if (open.scene.isFinished() || open.entity == null || open.entity.isRemoved()) {
                iterator.remove();
            }
        }
        RequestsS2CPacket.Entry typing = getAnsweringEntry();
        UUID typingId = typing == null ? null : typing.id();
        if (typingId == null || !typingId.equals(answering)) {
            answering = typingId;
            answer = "";
            judgedAnswer = "";
            verdict = PartyStatus.UNANSWERED;
            isHinting = false;
            answerLine = new TypedLine();
            hintLine = new TypedLine();
        }
        answerLine.tick(ANSWER_Y);
        hintLine.tick(HINT_Y);
    }

    private static List<ReadingScene.Line> getLines(RequestsS2CPacket.Entry entry) {
        String text = entry.role() == RequestsS2CPacket.ROLE_COOLDOWN ? getCooldownText(entry) : I18n.get(entry.key(), entry.args().toArray());
        if (entry.role() == RequestsS2CPacket.ROLE_RESULT) {
            boolean isApproved = entry.outcome() == Request.Outcome.APPROVED.ordinal();
            text = I18n.get(isApproved ? "request.cabalist.approved" : "request.cabalist.denied", text);
        }
        List<ReadingScene.Line> lines = new ArrayList<>();
        int textColor = ColorHelper.lighten(entry.color());
        for (String line : wrap(text.toLowerCase(Locale.ROOT), WRAP)) {
            lines.add(new ReadingScene.Line(line, textColor));
        }
        for (String change : entry.details()) {
            for (String line : wrap(change.toLowerCase(Locale.ROOT), WRAP)) {
                lines.add(new ReadingScene.Line(line, getChangeColor(change)));
            }
        }
        if (entry.role() != RequestsS2CPacket.ROLE_ANSWER) {
            for (RequestsS2CPacket.Party party : entry.parties()) {
                String status = I18n.get("request.cabalist.status." + PartyStatus.values()[party.status()].name().toLowerCase(Locale.ROOT));
                lines.add(new ReadingScene.Line((party.name() + " " + status).toLowerCase(Locale.ROOT), party.color()));
            }
        }
        return lines;
    }

    private static String getCooldownText(RequestsS2CPacket.Entry entry) {
        ClientLevel level = Minecraft.getInstance().level;
        long until = entry.args().size() > 1 ? Long.parseLong(entry.args().get(1)) : 0;
        long seconds = level == null ? 0 : Math.max(0, (until - level.getGameTime()) / 20);
        return I18n.get(entry.key(), entry.args().get(0), String.format("%d:%02d", seconds / 60, seconds % 60));
    }

    private static int getChangeColor(String change) {
        if (change.startsWith("+")) {
            return ADDED;
        }
        if (change.startsWith("-")) {
            return REMOVED;
        }
        return change.startsWith("~") ? EDITED : HINT;
    }

    static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (!line.isEmpty() && line.length() + 1 + word.length() > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private @Nullable RequestsS2CPacket.Entry getAnsweringEntry() {
        return NotificationCircles.INSTANCE.getAnsweringEntry();
    }

    // Your own open circles also show faintly through blocks, like their circles do.
    public void render(MatrixStack pose, Camera camera, float partialTick) {
        if (scenes.isEmpty()) {
            return;
        }
        GlyphRenderer glyphs = GlyphRenderer.INSTANCE.begin(GlyphRenderer.Depth.TESTED);
        renderScenes(glyphs, pose, camera, partialTick, false, 1);
        glyphs.end();
        glyphs = GlyphRenderer.INSTANCE.begin(GlyphRenderer.Depth.OCCLUDED);
        renderScenes(glyphs, pose, camera, partialTick, true, THROUGH_BLOCKS_ALPHA);
        glyphs.end();
    }

    private void renderScenes(GlyphRenderer glyphs, MatrixStack pose, Camera camera, float partialTick, boolean isOwnOnly, float opacity) {
        Vec3 right = new Vec3(camera.getLeftVector()).scale(-1);
        Vec3 up = new Vec3(camera.getUpVector());
        Entity self = Minecraft.getInstance().player;
        for (Open open : scenes.values()) {
            MagicCircle circle = open.entity == null || open.entry == null ? null : NotificationCircles.INSTANCE.getCircle(open.entity, open.entry, partialTick);
            if (circle == null || isOwnOnly && open.entity != self) {
                continue;
            }
            Vec3 center = circle.center().subtract(camera.getPosition());
            open.scene.render(glyphs, pose, center, right, up, partialTick, opacity);
            if (open.entry.id().equals(answering) && open.entity == self) {
                boolean isValid = judgedAnswer.equals(answer) && verdict != PartyStatus.UNANSWERED;
                answerLine.render(glyphs, pose, center, right, up, isValid ? VALID_ANSWER : INVALID_ANSWER, opacity, partialTick);
                hintLine.render(glyphs, pose, center, right, up, HINT, opacity, partialTick);
            }
        }
    }

    public boolean onKey(long window, int key, int action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (window != minecraft.getWindow().getWindow() || minecraft.screen != null || !NotificationCircles.INSTANCE.hasOpen()) {
            return false;
        }
        boolean isTyping = answering != null;
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (action == GLFW.GLFW_PRESS) {
                NotificationCircles.INSTANCE.closeLatest();
            }
            return true;
        }
        // CTRL hands the keys back to the game while casting
        if (!isTyping || ClientCastingSystem.isHandingKeysBack(window, key, action) || key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) {
            return false;
        }
        if (action == GLFW.GLFW_RELEASE) {
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> setAnswer(answer.isEmpty() ? "" : answer.substring(0, answer.length() - 1));
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> VeilPacketManager.server().sendPacket(new RequestAnswerC2SPacket(answering, answer, true));
            default -> {
            }
        }
        return true;
    }

    public boolean onChar(long window, int codePoint) {
        Minecraft minecraft = Minecraft.getInstance();
        if (window != minecraft.getWindow().getWindow() || minecraft.screen != null || answering == null || ClientCastingSystem.isControlHeld(window)) {
            return false;
        }
        char character = Character.toLowerCase((char) codePoint);
        if ((Character.isLetter(character) || character == ' ' || character == '\'') && answer.length() < MAX_ANSWER) {
            setAnswer(answer + character);
        }
        return true;
    }

    private void setAnswer(String text) {
        answer = text;
        answerLine.setText(text);
        if (answering != null) {
            VeilPacketManager.server().sendPacket(new RequestAnswerC2SPacket(answering, text, false));
        }
    }

    // An answer that can't be read as yes or no is cleared and the hint shows
    public void onVerdict(RequestVerdictS2CPacket packet) {
        if (!packet.id().equals(answering)) {
            return;
        }
        judgedAnswer = packet.text();
        verdict = PartyStatus.values()[packet.verdict()];
        if (!packet.isSubmit()) {
            return;
        }
        RequestsS2CPacket.Entry entry = getAnsweringEntry();
        if (verdict != PartyStatus.UNANSWERED && entry != null) {
            NotificationCircles.INSTANCE.close(entry);
        } else if (verdict == PartyStatus.UNANSWERED) {
            setAnswer("");
            isHinting = true;
            hintLine.setText(I18n.get("request.cabalist.answer_hint"));
        }
    }

    private record SceneKey(int entityId, UUID id, int role) {
    }

    private static class Open {
        private final ReadingScene scene = new ReadingScene();
        private @Nullable Entity entity;
        private @Nullable RequestsS2CPacket.Entry entry;
        private boolean isSeen;
    }
}
