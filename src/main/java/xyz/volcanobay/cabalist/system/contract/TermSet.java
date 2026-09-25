package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class TermSet extends HashMap<ResourceLocation, Set<Term>> {
    public void add(Term term) {
        Set<Term> terms = this.computeIfAbsent(term.getResourceLocation(), (resourceLocation -> new LinkedHashSet<>()));
        terms.add(term);
    }

    public void addAll(TermSet other) {
        for (Set<Term> terms : other.values()) {
            for (Term term : terms) {
                add(term);
            }
        }
    }

    /**
     * Returns an immutable empty set if there are no terms of this type.
     */
    public @NotNull Set<Term> get(ResourceLocation resourceLocation) {
        return this.getOrDefault(resourceLocation, Set.of());
    }
}
