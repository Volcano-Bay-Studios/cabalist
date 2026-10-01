package xyz.volcanobay.cabalist.system.energy;

import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;

// One spell's use of a stack it shares with others. Energy comes and goes through the shared stack,
// but what the spell couldn't pay stays its own, so one spell falling short doesn't stop the rest.
public class SharedEnergyStack extends EnergyStack {
    private final EnergyStack shared;

    public SharedEnergyStack(EnergyStack shared) {
        this.shared = shared;
    }

    @Override
    public double get(EnergyType type) {
        return shared.get(type);
    }

    @Override
    public void give(EnergyType type, double amount) {
        shared.give(type, amount);
    }

    @Override
    public void take(EnergyType type, double amount) {
        if (amount <= 0) {
            return;
        }
        double remaining = amount - shared.extract(type, amount);
        EnergyType entropy = CabalistEnergyTypes.ENTROPY.get();
        if (remaining > 0 && type != entropy) {
            remaining -= shared.extract(entropy, remaining);
        }
        if (remaining > 0) {
            addWorldPressure(remaining);
        }
    }

    @Override
    public double extract(EnergyType type, double max) {
        return shared.extract(type, max);
    }

    @Override
    public double convert(EnergyType from, EnergyType to, double amount) {
        return shared.convert(from, to, amount);
    }
}
