package xyz.volcanobay.cabalist.system.casting;

public enum CastingPhase {
    CHARGING,
    CHARGED,
    WRITING,
    AIMING;

    private static final CastingPhase[] VALUES = values();

    public static CastingPhase byId(int id) {
        return VALUES[Math.floorMod(id, VALUES.length)];
    }

    public boolean isWriting() {
        return this == WRITING || this == AIMING;
    }
}
