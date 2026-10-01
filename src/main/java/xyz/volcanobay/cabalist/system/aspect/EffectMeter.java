package xyz.volcanobay.cabalist.system.aspect;

public final class EffectMeter {
    private static double measured = Double.NaN;

    public static void begin() {
        measured = Double.NaN;
    }

    public static void report(double amount) {
        measured = amount;
    }

    public static double measure(double intended) {
        return Double.isNaN(measured) ? intended : measured;
    }
}
