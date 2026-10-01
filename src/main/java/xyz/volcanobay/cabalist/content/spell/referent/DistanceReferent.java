package xyz.volcanobay.cabalist.content.spell.referent;

import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;
import xyz.volcanobay.cabalist.system.subject.PositionSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

public class DistanceReferent extends SpellComponent {
    private static final SpellRole[] NEEDS = {SpellRole.NUMBER};
    private static final double DEFAULT_DISTANCE = 1;

    @Override
    public SpellRole getRole() {
        return SpellRole.REFERENT;
    }

    @Override
    public SpellRole[] getNeeds() {
        return NEEDS;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        Word number = word.getClaimed(SpellRole.NUMBER);
        double distance = DEFAULT_DISTANCE;
        if (number != null) {
            distance = number.getNumber();
            word.setMeasure(distance);
        }
        Subject caster = word.getClause().getCaster();
        Vector3d position = new Vector3d();
        Vector3d facing = new Vector3d();
        caster.getCastOrigin(position);
        caster.getFacing(facing);
        position.fma(distance, facing);
        word.setSubject(new PositionSubject(caster.getLevel(), position, facing));
    }
}
