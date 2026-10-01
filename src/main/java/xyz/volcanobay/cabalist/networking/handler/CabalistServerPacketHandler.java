package xyz.volcanobay.cabalist.networking.handler;

import foundry.veil.api.network.VeilPacketManager;
import foundry.veil.api.network.handler.ServerPacketContext;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.networking.packet.CastingDismissC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.CastingInputC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.LookTargetC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.RequestActionC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.RequestAnswerC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.RequestVerdictS2CPacket;
import xyz.volcanobay.cabalist.system.casting.CastingSystem;
import xyz.volcanobay.cabalist.system.contract.WorldContractee;
import xyz.volcanobay.cabalist.system.request.ConsentWords;
import xyz.volcanobay.cabalist.system.request.NoticeSystem;
import xyz.volcanobay.cabalist.system.request.PartyStatus;
import xyz.volcanobay.cabalist.system.request.Request;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.request.WorldConsent;
import xyz.volcanobay.cabalist.system.subject.NoticeCircleSubject;
import xyz.volcanobay.cabalist.system.subject.RequestCircleSubject;

import java.util.UUID;

public class CabalistServerPacketHandler {

    public static void handleCastingInput(CastingInputC2SPacket packet, ServerPacketContext ctx) {
        ctx.server().execute(() -> CastingSystem.INSTANCE.handle(ctx.player(), packet));
    }

    public static void handleRequestAction(RequestActionC2SPacket packet, ServerPacketContext ctx) {
        ctx.server().execute(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            Request request = RequestSystem.INSTANCE.get(packet.id());
            if (packet.action() == RequestActionC2SPacket.DISMISS) {
                dismiss(player, packet.id(), request);
            } else if (packet.action() == RequestActionC2SPacket.CLOSE) {
                RequestSystem.INSTANCE.setOpen(player, packet.id(), false);
            } else if (request != null && canView(player, request)) {
                RequestSystem.INSTANCE.markRead(request, player.getUUID());
                RequestSystem.INSTANCE.setOpen(player, request.getId(), true);
            }
        });
    }

    public static void handleRequestAnswer(RequestAnswerC2SPacket packet, ServerPacketContext ctx) {
        ctx.server().execute(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            Request request = RequestSystem.INSTANCE.get(packet.id());
            PartyStatus verdict = ConsentWords.judge(packet.text());
            if (packet.isSubmit() && request != null && verdict != PartyStatus.UNANSWERED) {
                if (request.needsAnswerFrom(player.getUUID())) {
                    RequestSystem.INSTANCE.answer(request, player.getUUID(), verdict, null);
                } else if (WorldConsent.isActive(player)) {
                    RequestSystem.INSTANCE.answer(request, WorldContractee.WORLD_ID, verdict, player.getUUID());
                }
                RequestSystem.INSTANCE.setOpen(player, request.getId(), false);
            }
            VeilPacketManager.player(player).sendPacket(new RequestVerdictS2CPacket(packet.id(), packet.text(), verdict.ordinal(), packet.isSubmit()));
        });
    }

    private static void dismiss(ServerPlayer player, UUID id, @Nullable Request request) {
        NoticeSystem.Notice notice = NoticeSystem.INSTANCE.get(id);
        if (notice != null) {
            new NoticeCircleSubject(notice, player, player.position()).dismiss(player.getUUID());
        } else if (request != null) {
            boolean isResult = request.getOutcome() != Request.Outcome.PENDING;
            new RequestCircleSubject(request, player, isResult, player.position()).answer(PartyStatus.DISPELLED, player.getUUID());
        }
    }

    private static boolean canView(ServerPlayer player, Request request) {
        return request.involves(player.getUUID()) || WorldConsent.isActive(player) && request.getParties().containsKey(WorldContractee.WORLD_ID);
    }

    public static void handleLookTarget(LookTargetC2SPacket packet, ServerPacketContext ctx) {
        ctx.server().execute(() -> CastingSystem.INSTANCE.setLookTarget(ctx.player(), packet));
    }

    public static void handleCastingDismiss(CastingDismissC2SPacket packet, ServerPacketContext ctx) {
        ctx.server().execute(() -> CastingSystem.INSTANCE.dismiss(ctx.player(), packet));
    }
}
