package xyz.volcanobay.cabalist.system.energy;

public class EnergyType {
    private static int count = 0;

    private final int id;

    public EnergyType() {
        this.id = count++;
    }

    public static int getCount() {
        return count;
    }

    public int getId() {
        return id;
    }
}
