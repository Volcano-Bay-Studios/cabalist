package xyz.volcanobay.cabalist.content.spell.form;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.form.InstantForm;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.subject.SubjectList;

import java.util.List;

/**
 * Used when a clause has no target. Lands on something random near where it was cast, usually the caster.
 */
public class WanderForm extends InstantForm {
    private static final double RADIUS = 4;

    @Override
    public void collectSubjects(SpellClause clause, SubjectList out) {
        Subject location = clause.getLocation();
        Level level = location.getLevel();
        if (level == null) {
            out.add(location);
            return;
        }
        Vector3d origin = new Vector3d();
        location.getCastOrigin(origin);
        AABB area = new AABB(origin.x - RADIUS, origin.y - RADIUS, origin.z - RADIUS, origin.x + RADIUS, origin.y + RADIUS, origin.z + RADIUS);
        List<Entity> nearby = level.getEntities((Entity) null, area);
        int locationCount = 1;
        for (Entity entity : nearby) {
            if (location.represents(entity)) {
                locationCount = 0;
            }
        }
        int pick = level.getRandom().nextInt(nearby.size() + locationCount);
        if (pick >= nearby.size()) {
            out.add(location);
            return;
        }
        out.add(EntitySubject.of(nearby.get(pick)));
    }
}
