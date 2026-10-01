package xyz.volcanobay.cabalist.content.spell.referent;

import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;
import xyz.volcanobay.cabalist.system.subject.PositionSubject;

public class LocalReferent extends SpellComponent {

    @Override
    public SpellRole getRole() {
        return SpellRole.REFERENT;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        Vector3d position = new Vector3d();
        resolver.getCaster().getPosition(position);
        PositionSubject positionSubject = PositionSubject.ofBlock(resolver.getCaster().getLevel(), new BlockPos((int) position.x, (int) position.y, (int) position.z));
        word.setSubject(positionSubject);
    }
}
