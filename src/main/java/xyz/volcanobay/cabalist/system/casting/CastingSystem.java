package xyz.volcanobay.cabalist.system.casting;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.networking.packet.CastingDismissC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.CastingInputC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.LookTargetC2SPacket;
import xyz.volcanobay.cabalist.system.focus.Focus;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class CastingSystem {
    public static final CastingSystem INSTANCE = new CastingSystem();

    private final Map<UUID, CastingSession> sessions = new HashMap<>();

    public @Nullable CastingSession get(ServerPlayer player) {
        return sessions.get(player.getUUID());
    }

    public void handle(ServerPlayer player, CastingInputC2SPacket packet) {
        CastingSession session = sessions.get(player.getUUID());
        if (packet.action() == CastingInputC2SPacket.HOLD) {
            if (session != null) {
                session.hold();
                return;
            }
            Focus focus = Focus.of(player);
            if (focus != null) {
                sessions.put(player.getUUID(), new CastingSession(player, focus));
            }
            return;
        }
        if (session == null) {
            return;
        }
        switch (packet.action()) {
            case CastingInputC2SPacket.RELEASE -> {
                if (!session.release()) {
                    sessions.remove(player.getUUID());
                }
            }
            case CastingInputC2SPacket.CAST -> {
                session.cast();
                sessions.remove(player.getUUID());
            }
            case CastingInputC2SPacket.ABANDON -> {
                sessions.remove(player.getUUID());
                session.end(false);
            }
            case CastingInputC2SPacket.PROMPT_ANSWER -> session.answer(packet.text());
            default -> session.edit(packet.action(), packet.seq(), packet.text());
        }
    }

    public void dismiss(ServerPlayer player, CastingDismissC2SPacket packet) {
        CastingSession session = sessions.get(player.getUUID());
        if (session != null) {
            session.setDismissTarget(packet.target());
        }
    }

    public void setLookTarget(ServerPlayer player, LookTargetC2SPacket packet) {
        CastingSession session = sessions.get(player.getUUID());
        if (session != null) {
            session.setLookTarget(packet);
        }
    }

    public void speak(ServerPlayer player, String text, boolean isFinal) {
        if (!CabalistConfig.VOICE_ENABLED.getAsBoolean()) {
            return;
        }
        if (text.equals("{\"text\": \"\"}") || text.equals("the")) {
            return;
        }
        CastingSession session = sessions.get(player.getUUID());
        if (session != null) {
            session.speak(text, isFinal);
        }
    }

    public void tick(MinecraftServer server) {
        for (Iterator<CastingSession> iterator = sessions.values().iterator(); iterator.hasNext(); ) {
            if (!iterator.next().tick()) {
                iterator.remove();
            }
        }
    }

    public void clear() {
        sessions.clear();
    }
}
