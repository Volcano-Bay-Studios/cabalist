package xyz.volcanobay.cabalist.system.spell;

import it.unimi.dsi.fastutil.objects.Object2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

public class EnergyLedger {
    public static final String UPKEEP = "upkeep";
    public static final String RIFT = "rift";
    public static final String AMBIENT = "ambient";
    public static final String DISMISSED = "dismissed";
    public static final String WASTED = "wasted";
    public static final String BLOCKED = "blocked";
    private static final double SMOOTHING = 0.05;
    private static final int TICKS_PER_SECOND = 20;

    private final Object2DoubleMap<String> totals = new Object2DoubleLinkedOpenHashMap<>();
    private final Object2DoubleMap<String> rates = new Object2DoubleLinkedOpenHashMap<>();
    private final Object2DoubleMap<String> thisTick = new Object2DoubleLinkedOpenHashMap<>(); // we love fastutil
    private long lastTick = Long.MIN_VALUE;

    public void spend(String on, double amount) {
        if (amount == 0) {
            return;
        }
        totals.mergeDouble(on, amount, Double::sum);
        thisTick.mergeDouble(on, amount, Double::sum);
    }

    public void tick(long gameTime) {
        if (gameTime == lastTick) {
            return;
        }
        lastTick = gameTime;
        for (String on : totals.keySet()) {
            double perSecond = thisTick.getDouble(on) * TICKS_PER_SECOND;
            rates.put(on, rates.getDouble(on) * (1 - SMOOTHING) + perSecond * SMOOTHING);
        }
        thisTick.clear();
    }

    public Object2DoubleMap<String> getTotals() {
        return totals;
    }

    public double getRate(String on) {
        return rates.getDouble(on);
    }

    public double getTotalRate() {
        double sum = 0;
        for (double rate : rates.values()) {
            sum += rate;
        }
        return sum;
    }
}
