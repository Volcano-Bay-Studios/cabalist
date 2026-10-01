package xyz.volcanobay.cabalist.content.spell.referent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.casting.CastingSession;
import xyz.volcanobay.cabalist.system.casting.CastingSystem;
import xyz.volcanobay.cabalist.system.casting.LookTargets;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.PositionSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

// Whatever the caster is looking at: a block, an entity or a magic circle. A caster that can't look points at itself
public class ThisReferent extends SpellComponent {
    private static final double REACH = 24;

    @Override
    public SpellRole getRole() {
        return SpellRole.REFERENT;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        Subject caster = word.getClause().getCaster();
        Subject seen = caster instanceof EntitySubject entitySubject ? findLookedAt(entitySubject.getEntity()) : null;
        word.setSubject(seen != null ? seen : caster);
    }

    public static @Nullable Subject findLookedAt(Entity looker) {
        Level level = looker.level();
        Vec3 eye = looker.getEyePosition();
        Vec3 look = looker.getLookAngle();
        BlockHitResult block = level.clip(new ClipContext(eye, eye.add(look.scale(REACH)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, looker));
        double nearest = block.getType() == HitResult.Type.MISS ? REACH : block.getLocation().distanceTo(eye);
        Vec3 end = eye.add(look.scale(nearest));
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, looker, eye, end, looker.getBoundingBox().expandTowards(look.scale(nearest)).inflate(1),
                target -> !target.isSpectator() && target.isPickable());
        Subject seen = null;
        if (entity != null) {
            nearest = entity.getLocation().distanceTo(eye);
            seen = EntitySubject.of(entity.getEntity());
        } else if (block.getType() != HitResult.Type.MISS) {
            Vec3 center = block.getBlockPos().getCenter();
            seen = new PositionSubject(level, new Vector3d(center.x, center.y, center.z), new Vector3d(look.x, look.y, look.z));
        }
        CastingSession session = looker instanceof ServerPlayer player ? CastingSystem.INSTANCE.get(player) : null;
        if (session != null && session.getLookTarget().distance() < nearest) {
            Subject circle = LookTargets.resolve(looker, session.getLookTarget());
            if (circle != null) {
                return circle;
            }
        }
        return seen;
    }
}
