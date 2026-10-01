package xyz.volcanobay.cabalist.client.renderer.circle;

import net.minecraft.util.Mth;

/**
 * How a spell's energy flow shows on its circles. Shown by making spending faster spin them faster and brighter,
 * and a draining allotment dims then sputters.
 */
public final class CircleEnergy {
    private static final float FULL_FLOW = 20;
    private static final float IDLE_SPIN = 0.6f;
    private static final float EXTRA_SPIN = 2.4f;
    private static final float MIN_BRIGHTNESS = 0.4f;
    private static final float FLOW_BRIGHTNESS = 0.3f;
    private static final float SPUTTER_BELOW = 0.25f;
    private static final float SPUTTER_DEPTH = 0.6f;

    public static float getIntensity(float flow) {
        return Mth.clamp(flow / FULL_FLOW, 0, 1);
    }

    public static float getSpin(float flow) {
        return IDLE_SPIN + EXTRA_SPIN * getIntensity(flow);
    }

    public static float getBrightness(float flow, float remaining, float time) {
        float brightness = (MIN_BRIGHTNESS + (1 - MIN_BRIGHTNESS) * Mth.clamp(remaining, 0, 1)) * (1 - FLOW_BRIGHTNESS / 2 + FLOW_BRIGHTNESS * getIntensity(flow));
        if (remaining < SPUTTER_BELOW) {
            float flicker = 0.5f + 0.5f * Mth.sin(time * 1.9f) * Mth.sin(time * 3.7f);
            brightness *= 1 - (SPUTTER_BELOW - remaining) / SPUTTER_BELOW * SPUTTER_DEPTH * flicker;
        }
        return brightness;
    }
}
