package xyz.volcanobay.cabalist.system.request;

import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.system.spell.SpellEngine;

/**
 * Compares strings against consent and deny components.
 */
public class ConsentWords {
    public static PartyStatus judge(String text) {
        if (text.isBlank()) {
            return PartyStatus.UNANSWERED;
        }
        boolean isYes = false;
        boolean isNo = false;
        for (SpellEngine.Candidate span : SpellEngine.INSTANCE.parse(text).getSpans()) {
            isYes |= span.components().contains(CabalistSpellComponents.CONSENT.get());
            isNo |= span.components().contains(CabalistSpellComponents.DENY.get());
        }
        return isYes == isNo ? PartyStatus.UNANSWERED : isYes ? PartyStatus.YES : PartyStatus.NO;
    }
}
