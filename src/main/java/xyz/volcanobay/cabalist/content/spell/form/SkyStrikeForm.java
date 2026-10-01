package xyz.volcanobay.cabalist.content.spell.form;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.core.CabalistEntities;
import xyz.volcanobay.cabalist.entity.FallingBeam;
import xyz.volcanobay.cabalist.system.form.Form;
import xyz.volcanobay.cabalist.system.form.FormDimension;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.spell.SpellExecutor;

/**
 * A beam that falls from the sky onto the target, or onto where the caster is looking,
 * hitting everything in its column on the way down, then standing where it landed.
 */
public class SkyStrikeForm extends Form {
    private static final double RANGE = 32;
    private static final double HEIGHT = 64;
    private static final double SPEED = 2;
    private static final double RADIUS = 1.5;

    private static double getRadius(SpellClause clause) {
        return getDimension(clause, FormDimension.SIZE, RADIUS) / clause.getFormFactor(FormDimension.SIZE) * clause.getFormFactor(FormDimension.BREADTH);
    }

    private static double getHeight(SpellClause clause) {
        return HEIGHT * clause.getFormFactor(FormDimension.SIZE);
    }

    @Override
    public double getCostFactor(SpellClause clause) {
        double ratio = getRadius(clause) / RADIUS;
        return ratio * ratio;
    }

    @Override
    public boolean completesLater() {
        return true;
    }

    @Override
    public int deliver(SpellClause clause, SpellExecutor executor) {
        Level level = clause.getLocation().getLevel();
        if (level == null) {
            return 0;
        }
        Vector3d center = new Vector3d();
        if (clause.getTarget() != null || clause.getOrigin() != null) {
            getCenter(clause, center);
        } else {
            Vector3d from = new Vector3d();
            aim(clause, from, center, RANGE);
            BlockHitResult aimHit = level.clip(new ClipContext(new Vec3(from.x, from.y, from.z), new Vec3(center.x, center.y, center.z),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            center.set(aimHit.getLocation().x, aimHit.getLocation().y, aimHit.getLocation().z);
        }

        double height = getHeight(clause);
        FallingBeam strike = new FallingBeam(CabalistEntities.SKY_STRIKE.get(), level);
        strike.launch(clause, new Vec3(center.x, center.y + height, center.z), SPEED, getRadius(clause), height * 2);
        StrikeDelivery delivery = new StrikeDelivery(clause, level, strike, getRadius(clause), height);
        strike.setDelivery(delivery);
        level.addFreshEntity(strike);
        delivery.showVisual(delivery.getFallingShape());
        delivery.start();
        return 0;
    }
}
