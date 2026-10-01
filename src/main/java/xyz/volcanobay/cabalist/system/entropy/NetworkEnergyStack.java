package xyz.volcanobay.cabalist.system.entropy;

import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.energy.EnergyType;

public class NetworkEnergyStack extends EnergyStack {
    private final EntropyNetworkContract contract;

    public NetworkEnergyStack(EntropyNetworkContract contract) {
        this.contract = contract;
    }

    private static boolean isEntropy(EnergyType type) {
        return type == CabalistEnergyTypes.ENTROPY.get();
    }

    @Override
    public double get(EnergyType type) {
        if (!isEntropy(type)) {
            return super.get(type);
        }
        EntropyNetwork network = contract.getNetwork();
        return network == null ? 0 : network.getFreeEntropy();
    }

    @Override
    public void give(EnergyType type, double amount) {
        if (!isEntropy(type)) {
            super.give(type, amount);
            return;
        }
        EntropyNetwork network = contract.getNetwork();
        if (network != null && amount > 0) {
            network.addFreeEntropy(amount);
        }
    }

    @Override
    public void take(EnergyType type, double amount) {
        if (!isEntropy(type)) {
            super.take(type, amount);
            return;
        }
        double extracted = extract(type, amount);
        if (amount - extracted > 0) {
            addWorldPressure(amount - extracted);
        }
    }

    @Override
    public double extract(EnergyType type, double max) {
        if (!isEntropy(type)) {
            return super.extract(type, max);
        }
        EntropyNetwork network = contract.getNetwork();
        return network == null ? 0 : network.extractFreeEntropy(max);
    }

    @Override
    public double convert(EnergyType from, EnergyType to, double amount) {
        double moved = extract(from, amount);
        give(to, moved);
        return moved;
    }

}
