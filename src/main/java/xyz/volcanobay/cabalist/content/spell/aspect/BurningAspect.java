package xyz.volcanobay.cabalist.content.spell.aspect;

import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EffectMeter;
import xyz.volcanobay.cabalist.system.request.PartyStatus;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.DraftCircleSubject;
import xyz.volcanobay.cabalist.system.subject.NoticeCircleSubject;
import xyz.volcanobay.cabalist.system.subject.RequestCircleSubject;

// Fire burns circles: it removes a draft's line, dismisses a notice, and refuses a request as an act of aggression.
public abstract class BurningAspect extends Aspect {
    // What burning one circle counts as for the spell's cost.
    protected abstract double getCircleBurnEffect();

    @Override
    public boolean affectDraft(DraftCircleSubject circle, SpellClause clause, float magnitude) {
        if (!circle.remove(false, clause.getCaster())) {
            return false;
        }
        EffectMeter.report(getCircleBurnEffect());
        return true;
    }

    @Override
    public boolean affectNotice(NoticeCircleSubject circle, SpellClause clause, float magnitude) {
        return circle.dismiss(clause.getCaster().getUUID());
    }

    @Override
    public boolean affectRequest(RequestCircleSubject circle, SpellClause clause, float magnitude) {
        if (!circle.answer(PartyStatus.BURNED, clause.getCaster().getUUID())) {
            return false;
        }
        EffectMeter.report(getCircleBurnEffect());
        return true;
    }
}
