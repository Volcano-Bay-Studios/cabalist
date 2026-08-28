package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Contract implements Contractee {
    private final List<Contractee> contractees = new ArrayList<>();
    private final TermSet terms = new TermSet();
    private final UUID uuid = UUID.randomUUID();
    private final ArrayList<Contract> activeContracts = new ArrayList<>();

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public void tickContract(Contract contract) {
        for (Contractee contractee : contractees) {
            contractee.tickContract(contract);
        }
    }

    public void addContractee(Contractee contractee) {
        contractees.add(contractee);
    }

    public void addTerm(Term term) {
        terms.add(term);
    }

    public void tick() {
        for (Contractee contractee : contractees) {
            contractee.tickContract(this);
        }
    }

    @Override
    public TermSet getTerms() {
        return (TermSet) terms.clone();
    }

    @Override
    public List<Contract> getActiveContractsList() {
        return activeContracts;
    }

    public CompoundTag write(CompoundTag tag) {
        return tag;
    }

    public void read(CompoundTag tag) {
    }
}
