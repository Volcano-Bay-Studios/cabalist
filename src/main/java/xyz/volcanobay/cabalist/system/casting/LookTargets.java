package xyz.volcanobay.cabalist.system.casting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.networking.packet.LookTargetC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.RequestsS2CPacket;
import xyz.volcanobay.cabalist.system.contract.ContractDraft;
import xyz.volcanobay.cabalist.system.contract.DraftSystem;
import xyz.volcanobay.cabalist.system.form.DeliverySystem;
import xyz.volcanobay.cabalist.system.request.NoticeSystem;
import xyz.volcanobay.cabalist.system.request.Request;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.spell.HangingSpellSystem;
import xyz.volcanobay.cabalist.system.spell.PendingSpell;
import xyz.volcanobay.cabalist.system.subject.DraftCircleSubject;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.NoticeCircleSubject;
import xyz.volcanobay.cabalist.system.subject.RequestCircleSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

public class LookTargets {
    public static final double REACH = 24;

    public static @Nullable Subject resolve(Entity looker, LookTargetC2SPacket target) {
        if (target.kind() == LookTargetC2SPacket.NONE || target.at().distanceTo(looker.getEyePosition()) > REACH) {
            return null;
        }
        if (target.kind() == LookTargetC2SPacket.DRAFT) {
            ContractDraft draft = DraftSystem.INSTANCE.get(target.requestId());
            return draft == null ? null : new DraftCircleSubject(draft, target.index(), looker.level(), target.at());
        }
        if (target.kind() == LookTargetC2SPacket.FORM) {
            return DeliverySystem.INSTANCE.findSpell(target.index());
        }
        Entity host = looker.level().getEntity(target.hostId());
        if (host == null || host.distanceTo(looker) > REACH) {
            return null;
        }
        if (target.kind() == LookTargetC2SPacket.SPELL) {
            PendingSpell pending = HangingSpellSystem.INSTANCE.find(EntitySubject.of(host), target.index(), target.wordsHash());
            return pending == null ? null : pending.getSpell();
        }
        if (target.kind() == LookTargetC2SPacket.REQUEST && target.role() == RequestsS2CPacket.ROLE_INFO && host instanceof ServerPlayer owner) {
            NoticeSystem.Notice notice = NoticeSystem.INSTANCE.get(target.requestId());
            return notice == null ? null : new NoticeCircleSubject(notice, owner, target.at());
        }
        if (target.kind() == LookTargetC2SPacket.REQUEST && host instanceof ServerPlayer owner) {
            Request request = RequestSystem.INSTANCE.get(target.requestId());
            if (request != null) {
                return new RequestCircleSubject(request, owner, target.role() == RequestsS2CPacket.ROLE_RESULT, target.at());
            }
        }
        return null;
    }
}
