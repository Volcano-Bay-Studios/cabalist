package xyz.volcanobay.cabalist.system.contract;

import java.util.List;
import java.util.UUID;

public interface Contractee {
    /**
     * A unique identifier for the contractee.
     */
    UUID getUUID();

    /**
     * If this contractee may accept contracts.
     */
    default boolean valid() {
        return true;
    }

    /**
     * Used to apply contract terms to the contractee.
     */
    void tickContract(Contract contract);

    /**
     * Returns a set of terms this contractee applies.
     * This set will be mutated by the contract system.
     */
    default TermSet getTerms() {
        return new TermSet();
    }

    /**
     * Returns a new set of all terms this contractee should use, and all terms that active contracts apply to it.
     */
    default TermSet getAllAppliedTerms() {
        TermSet terms = getTerms();
        for (Contract contract : getActiveContractsList()) {
            terms.putAll(contract.getAllAppliedTerms());
        }
        return terms;
    }

    /**
     * Returns a list of all contracts this contractee is currently in.
     * This list must be mutable! Contracts will be added to this list with this method.
     */
    List<Contract> getActiveContractsList();
}
