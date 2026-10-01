package xyz.volcanobay.cabalist.system.spell;

/**
 * A registered part of a spell. Components are stateless singletons shared across threads,
 * so all per-cast state lives on {@link Word}.
 */
public abstract class SpellComponent {
    protected static final SpellRole[] NO_NEEDS = new SpellRole[0];

    public abstract SpellRole getRole();

    public SpellRole[] getNeeds() {
        return NO_NEEDS;
    }

    public int getPriority() {
        return 0;
    }

    /**
     * This will claim following text. Usually encapsulated by quotation marks.
     */
    public boolean claimsTrailingText() {
        return false;
    }

    /**
     * Splits spells into parts
     */
    public boolean splitsClauses() {
        return false;
    }

    public abstract void resolve(Word word, SpellResolver resolver);
}
