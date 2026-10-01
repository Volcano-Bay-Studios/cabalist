package xyz.volcanobay.cabalist.system.form;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.render.FormShape;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.spell.SpellExecutor;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.subject.SubjectList;

/**
 * Keeps working on the subjects it first reached, continues if they still exist
 */
public class PointDelivery extends Delivery {
    private static final float VISUAL_RADIUS = 0.5f;

    private final SubjectList subjects = new SubjectList();
    private final boolean chargesRift;

    public PointDelivery(SpellClause clause, Level level, SubjectList reached, boolean chargesRift) {
        super(clause, level);
        this.subjects.addAll(reached);
        this.chargesRift = chargesRift;
    }

    public int deliverNow() {
        Vector3d position = new Vector3d();
        for (Subject subject : subjects) {
            Level subjectLevel = subject.getLevel();
            if (subjectLevel != level) {
                continue;
            }
            if (subject instanceof EntitySubject entitySubject) {
                showVisual(FormShape.following(FormShape.Kind.POINT, entitySubject.getEntity().getId(), entitySubject.getEntity().position(), VISUAL_RADIUS, 0));
            } else {
                subject.getPosition(position);
                showVisual(FormShape.point(new Vec3(position.x, position.y + 1, position.z), 0));
            }
        }
        return apply(subjects);
    }

    private int apply(SubjectList reached) {
        int affected = SpellExecutor.INSTANCE.apply(clause, reached);
        if (chargesRift) {
            SpellExecutor.INSTANCE.chargeRift(clause);
        }
        return affected;
    }

    @Override
    protected boolean deliverTick() {
        return true;
    }

    @Override
    protected void collectSustained(SubjectList out) {
        for (Subject subject : subjects) {
            if (subject.isValid()) {
                out.add(subject);
            }
        }
    }

    @Override
    protected void applyBatch() {
        if (!batch.isEmpty()) {
            apply(batch);
            batch.clear();
        }
    }

    @Override
    protected boolean hasSustainedSubjects() {
        for (Subject subject : subjects) {
            if (subject.isValid()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Subject getEndLocation() {
        return subjects.isEmpty() ? clause.getLocation() : subjects.get(0);
    }
}
