package xyz.volcanobay.cabalist.system.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;
import xyz.volcanobay.cabalist.core.CabalistAttachments;
import xyz.volcanobay.cabalist.core.CabalistConfig;

// The health a living thing can spend on magic.
public final class Lifeforce {
    public static double get(LivingEntity living) {
        if (!living.hasData(CabalistAttachments.LIFEFORCE.get())) {
            return living.getHealth();
        }
        Data data = living.getData(CabalistAttachments.LIFEFORCE.get());
        long elapsed = Math.max(0, living.level().getGameTime() - data.time());
        return Math.min(living.getHealth(), data.amount() + elapsed * getRecoveryPerTick(living));
    }

    public static void set(LivingEntity living, double amount) {
        living.setData(CabalistAttachments.LIFEFORCE.get(), new Data(Math.clamp(amount, 0, living.getHealth()), living.level().getGameTime()));
    }

    public static void settle(LivingEntity living) {
        set(living, get(living));
    }

    private static double getRecoveryPerTick(LivingEntity living) {
        double seconds = CabalistConfig.LIFEFORCE_RECOVERY_SECONDS.get();
        return seconds <= 0 ? Double.MAX_VALUE : living.getMaxHealth() / (seconds * 20);
    }

    public record Data(double amount, long time) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("amount").forGetter(Data::amount),
                Codec.LONG.fieldOf("time").forGetter(Data::time)
        ).apply(instance, Data::new));
    }
}
