package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class ContractManager {
    /**
     * Contracts do not belong to any level.
     */
    public static final ContractManager INSTANCE = new ContractManager();

    private final List<Contract> contracts = new ArrayList<>();

    private ContractManager() {}

    public void addContract(Contract contract) {
        contracts.add(contract);
    }

    public List<Contract> getContracts() {
        return contracts;
    }

    public void tick() {
        for (Contract contract : contracts) {
            contract.tick();
        }
    }

    public CompoundTag write(CompoundTag tag) {
        return tag;
    }

    public void read(CompoundTag tag) {

    }
}
