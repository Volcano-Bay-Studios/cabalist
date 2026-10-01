package xyz.volcanobay.cabalist.system.focus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

import java.util.HashMap;
import java.util.Map;

/**
 * Energy built up while casting. It comes quickly at first and slows the longer it goes. chars increase the max
 */
public class Gathering {
    private static final double SIMULATED_STEP_SECONDS = 0.05;
    private static final double FADED_WEIGHT = 0.25;

    private final Focus focus;
    private @Nullable Domain domain;
    private double fromImbued;
    private double drawn;
    private double seconds;
    private int charsUsed;
    private int charsFaded;
    private double capacityMultiplier = 1;

    public Gathering(Focus focus, @Nullable Domain domain) {
        this.focus = focus;
        this.domain = domain;
    }

    public static Gathering simulate(Focus focus, Spell spell, String utterance) {
        Gathering gathering = new Gathering(focus, getDomain(spell));
        gathering.setChars(utterance.replace(" ", "").length(), 0);
        gathering.setCapacityMultiplier(spell.getCapacityMultiplier());
        double total = focus.getChargeSeconds() + spell.getCastSeconds();
        for (double elapsed = 0; elapsed < total; elapsed += SIMULATED_STEP_SECONDS) {
            gathering.tick(Math.min(SIMULATED_STEP_SECONDS, total - elapsed));
        }
        return gathering;
    }

    public static @Nullable Domain getDomain(Spell spell) {
        for (SpellClause clause : spell.getClauses()) {
            Domain domain = clause.getPrimaryDomain();
            if (domain != null) {
                return domain;
            }
        }
        return null;
    }

    public void setChars(int used, int faded) {
        charsUsed = used;
        charsFaded = faded;
    }

    public void setDomain(@Nullable Domain domain) {
        this.domain = domain;
    }

    public double getRateFraction() {
        double throughput = focus.getThroughput(domain);
        return throughput <= 0 ? 0 : getRate() / throughput;
    }

    public boolean isFull() {
        return getGathered() >= getCapacity();
    }

    public void setCapacityMultiplier(double capacityMultiplier) {
        this.capacityMultiplier = capacityMultiplier;
    }

    public void tick(double deltaSeconds) {
        double amount = Math.min(getCapacity() - getGathered(), getRate() * deltaSeconds);
        seconds += deltaSeconds;
        if (amount <= 0) {
            return;
        }
        fromImbued += amount;
    }

    public void draw(double amount) {
        drawn += Math.max(0, Math.min(amount, getCapacity() - getGathered()));
    }

    public double push(double amount) {
        double gathered = getGathered();
        if (gathered <= 0 || amount <= 0) {
            return 0;
        }
        double kept = 1 - Math.min(1, amount / gathered);
        fromImbued *= kept;
        drawn *= kept;
        return gathered * (1 - kept);
    }

    public double getThroughput() {
        return focus.getThroughput(domain);
    }

    public double getRate() {
        return focus.getThroughput(domain) / (1 + seconds / CabalistConfig.GATHER_SLOWDOWN_SECONDS.get());
    }

    public double getCapacity() {
        double chars = charsUsed + charsFaded * FADED_WEIGHT;
        return CabalistConfig.CAPACITY_PER_CHAR.get() * Math.pow(chars, CabalistConfig.CAPACITY_EXPONENT.get()) * capacityMultiplier;
    }

    public double getGathered() {
        return fromImbued + drawn;
    }

    public double getSeconds() {
        return seconds;
    }

    public double getOverflow() {
        return Math.max(0, fromImbued - focus.getSafeMaximum());
    }

    public SpellSource getSource() {
        Map<ResourceLocation, Double> lifeforce = focus.getCombined().lifeforce();
        double total = 0;
        for (double amount : lifeforce.values()) {
            total += amount;
        }
        Map<ResourceLocation, Double> imbued = new HashMap<>();
        if (total > 0) {
            double scale = fromImbued / total;
            lifeforce.forEach((type, amount) -> imbued.put(type, amount * scale));
        }
        return new SpellSource(imbued);
    }

    public void applyTo(Spell spell, @Nullable LivingEntity caster) {
        spell.getEnergyStack().give(CabalistEnergyTypes.ENTROPY.get(), getGathered());
        spell.setSource(getSource());
        focus.attune(domain, getGathered());
        double overflow = getOverflow();
        if (caster != null && overflow > 0) {
            caster.hurt(caster.damageSources().magic(), (float) (overflow / CabalistConfig.ENTROPY_PER_HEALTH.get()));
        }
    }
}
