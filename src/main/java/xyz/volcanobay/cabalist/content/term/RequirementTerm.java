package xyz.volcanobay.cabalist.content.term;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.system.contract.Term;
import xyz.volcanobay.cabalist.system.spell.TriggerEvent;
import xyz.volcanobay.cabalist.system.subject.Subject;

/**
 * Met when a trigger of the same id happens to the spell's host, or to the subject it watches instead, like "when my enemy dies".
 */
public class RequirementTerm extends Term {
    private @Nullable Subject watched;
    private boolean isWatchingNothing;

    public RequirementTerm(ResourceLocation resourceLocation) {
        super(resourceLocation);
    }

    public void watch(@Nullable Subject watched) {
        this.watched = watched;
    }

    public void watchNothing() {
        isWatchingNothing = true;
    }

    public boolean isAboutHost(Subject host) {
        return !isWatchingNothing && (watched == null || watched.equals(host));
    }

    @Override
    public boolean isMetBy(TriggerEvent event, Subject host) {
        return !isWatchingNothing && event.host().equals(watched != null ? watched : host);
    }

    @Override
    public Term create() {
        return new RequirementTerm(getResourceLocation());
    }
}
