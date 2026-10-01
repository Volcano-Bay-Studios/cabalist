package xyz.volcanobay.cabalist.content.spell.component;

import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;

import java.util.function.Supplier;

/**
 * "invite", "expel": claims the nearest referent as the one joining or leaving.
 */
public class MembershipComponent extends SpellComponent {
    private static final SpellRole[] NEEDS = {SpellRole.REFERENT};

    private final Supplier<Aspect> aspect;

    public MembershipComponent(Supplier<Aspect> aspect) {
        this.aspect = aspect;
    }

    @Override
    public SpellRole getRole() {
        return SpellRole.PETITION;
    }

    @Override
    public SpellRole[] getNeeds() {
        return NEEDS;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        word.getClause().addAspect(aspect.get());
        Word member = word.getClaimed(SpellRole.REFERENT);
        if (member != null) {
            word.getClause().setContractMember(member.getSubject());
        }
    }
}
