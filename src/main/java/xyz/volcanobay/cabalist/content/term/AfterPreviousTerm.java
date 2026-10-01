package xyz.volcanobay.cabalist.content.term;

import net.minecraft.resources.ResourceLocation;
import xyz.volcanobay.cabalist.system.contract.Term;

/**
 * A clause condition: the clause waits until the previous clause has fired.
 */
public class AfterPreviousTerm extends Term {

    public AfterPreviousTerm(ResourceLocation resourceLocation) {
        super(resourceLocation);
    }

    @Override
    public Term create() {
        return new AfterPreviousTerm(getResourceLocation());
    }
}
