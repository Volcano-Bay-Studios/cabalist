package xyz.volcanobay.cabalist.content.spell.referent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import xyz.volcanobay.cabalist.system.blood.BloodStain;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;
import xyz.volcanobay.cabalist.system.subject.BloodSubject;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

public class BloodReferent extends SpellComponent {
    private static final SpellRole[] NEEDS = {SpellRole.REFERENT};

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
        return 20;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        Word owner = word.getClaimed(SpellRole.REFERENT);
        Subject bearer = word.getClause().getCaster();
        if (owner != null && owner.getSubject() != null) {
            bearer = owner.getSubject();
        }
        BloodStain blood = bearer.getBlood();
        if (blood == null || !(bearer.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity bleeder = level.getServer().getPlayerList().getPlayer(blood.owner());
        if (bleeder == null) {
            bleeder = level.getEntity(blood.owner());
        }
        if (bleeder != null) {
            word.setSubject(new BloodSubject(EntitySubject.of(bleeder), blood.consented()));
        }
    }
}
