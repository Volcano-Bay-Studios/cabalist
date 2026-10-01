package xyz.volcanobay.cabalist.system.subject;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.request.NoticeSystem;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

import java.util.UUID;

/**
 * Notification Circle. Can only be dismissed by the owner.
 */
public class NoticeCircleSubject extends Subject {
    private final NoticeSystem.Notice notice;
    private final ServerPlayer owner;
    private final Vec3 at;

    public NoticeCircleSubject(NoticeSystem.Notice notice, ServerPlayer owner, Vec3 at) {
        this.notice = notice;
        this.owner = owner;
        this.at = at;
    }

    public boolean dismiss(@Nullable UUID caster) {
        if (!owner.getUUID().equals(caster)) {
            return false;
        }
        NoticeSystem.INSTANCE.remove(notice);
        return true;
    }

    @Override
    public boolean isValid() {
        return !owner.isRemoved() && NoticeSystem.INSTANCE.get(notice.id()) != null;
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
        out.set(0, 1, 0);
    }

    @Override
    public boolean receive(Aspect aspect, SpellClause clause, float magnitude) {
        return aspect.affectNotice(this, clause, magnitude);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(notice.title());
    }
}
