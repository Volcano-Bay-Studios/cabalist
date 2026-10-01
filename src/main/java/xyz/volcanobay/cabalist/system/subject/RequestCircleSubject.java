package xyz.volcanobay.cabalist.system.subject;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.WorldContractee;
import xyz.volcanobay.cabalist.system.request.PartyStatus;
import xyz.volcanobay.cabalist.system.request.Request;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.request.WorldConsent;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

import java.util.UUID;

public class RequestCircleSubject extends Subject {
    private final Request request;
    private final ServerPlayer owner;
    private final boolean isResult;
    private final Vec3 at;

    public RequestCircleSubject(Request request, ServerPlayer owner, boolean isResult, Vec3 at) {
        this.request = request;
        this.owner = owner;
        this.isResult = isResult;
        this.at = at;
    }

    // Burning a result opens a dispute, 
    // TODO: dispute system!!!
    public boolean answer(PartyStatus status, @Nullable UUID caster) {
        if (!owner.getUUID().equals(caster)) {
            return false;
        }
        RequestSystem requests = RequestSystem.INSTANCE;
        if (isResult) {
            if (status == PartyStatus.BURNED) {
                Contract contract = request.getContract();
                if (contract != null) {
                    contract.logAction(caster, "dispute", request.getKind().name().toLowerCase(), "opened");
                }
            } else if (status != PartyStatus.DISPELLED) {
                return false;
            }
            requests.dismiss(request, caster);
            return true;
        }
        if (request.needsAnswerFrom(caster)) {
            return requests.answer(request, caster, status, null);
        }
        if (WorldConsent.isActive(owner) && request.needsAnswerFrom(WorldContractee.WORLD_ID)) {
            return requests.answer(request, WorldContractee.WORLD_ID, status, caster);
        }
        return false;
    }

    @Override
    public boolean isValid() {
        if (owner.isRemoved() || RequestSystem.INSTANCE.get(request.getId()) == null) {
            return false;
        }
        boolean isPending = request.getOutcome() == Request.Outcome.PENDING;
        return isResult ? !isPending && !request.isDismissed(owner.getUUID()) : isPending;
    }

    @Override
    public @Nullable Level getLevel() {
        return owner.level();
    }

    @Override
    public void getPosition(Vector3d out) {
        out.set(at.x, at.y, at.z);
    }

    @Override
    public void getFacing(Vector3d out) {
        Vec3 look = owner.getLookAngle();
        out.set(look.x, look.y, look.z);
    }

    @Override
    public boolean receive(Aspect aspect, SpellClause clause, float magnitude) {
        return aspect.affectRequest(this, clause, magnitude);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Request upon ").append(owner.getDisplayName());
    }
}
