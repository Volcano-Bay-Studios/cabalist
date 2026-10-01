package xyz.volcanobay.cabalist.system.aspect;

import xyz.volcanobay.cabalist.system.spell.Spell;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * For costs paid once a tick per spell, however many subjects the spell reaches that tick.
 */
public class OncePerTick {
    private final Map<Spell, Long> lastAt = Collections.synchronizedMap(new WeakHashMap<>());

    public boolean tryPass(Spell spell, long gameTime) {
        Long last = lastAt.put(spell, gameTime);
        return last == null || last != gameTime;
    }
}
