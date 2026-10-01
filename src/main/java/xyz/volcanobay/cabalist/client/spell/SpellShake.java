package xyz.volcanobay.cabalist.client.spell;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.util.Easing;
import foundry.veil.api.screenshake.type.LocalScreenShake;
import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.networking.packet.SpellShakeS2CPacket;

/**
 * Shakes the screen by how much energy a spell just moved, with diminishing returns, scaled by the vanilla screen effect setting.
 */
public class SpellShake {
    private static final double ENERGY_FOR_FULL_SHAKE = 60;
    private static final float MAX_STRENGTH = 0.35f;
    private static final int BASE_LENGTH_TICKS = 6;
    private static final int EXTRA_LENGTH_TICKS = 14;
    private static final int BATCH_TICKS = 5;

    private static double pendingEnergy;
    private static Vec3 weightedPosition = Vec3.ZERO;
    private static int ticksUntilFlush = BATCH_TICKS;

    public static void add(Vec3 at, float energy) {
        weightedPosition = weightedPosition.add(at.scale(energy));
        pendingEnergy += energy;
    }

    public static void tick() {
        if (--ticksUntilFlush > 0) {
            return;
        }
        ticksUntilFlush = BATCH_TICKS;
        if (pendingEnergy <= 0) {
            return;
        }
        Vec3 at = weightedPosition.scale(1 / pendingEnergy);
        double energy = pendingEnergy;
        pendingEnergy = 0;
        weightedPosition = Vec3.ZERO;
        shake(at, energy);
    }

    private static void shake(Vec3 at, double energy) {
        float scale = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        float intensity = (float) Math.min(1, Math.sqrt(energy / ENERGY_FOR_FULL_SHAKE));
        float strength = MAX_STRENGTH * intensity * scale;
        if (strength <= 0.001f) {
            return;
        }
        int length = BASE_LENGTH_TICKS + Math.round(EXTRA_LENGTH_TICKS * intensity);
        try {
            VeilRenderSystem.renderer().getScreenShakeManager().addScreenShake(new LocalScreenShake(
                    strength + " * (1 - q.agePercent)", at, length, (float) SpellShakeS2CPacket.RANGE, Easing.EASE_OUT_QUAD));
        } catch (MolangSyntaxException e) {
            Cabalist.LOGGER.error("Invalid spell screen shake expression", e);
        }
    }
}
