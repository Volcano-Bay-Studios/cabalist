package xyz.volcanobay.cabalist.system.focus;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record Imbuement(Map<ResourceLocation, Double> lifeforce, Map<ResourceLocation, Double> attunement, boolean receptive, int seed) {
    public static final Imbuement EMPTY = new Imbuement(Map.of(), Map.of(), false, 0);

    private static final Codec<Map<ResourceLocation, Double>> AMOUNTS = Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE);

    public static final Codec<Imbuement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AMOUNTS.optionalFieldOf("lifeforce", Map.of()).forGetter(Imbuement::lifeforce),
            AMOUNTS.optionalFieldOf("attunement", Map.of()).forGetter(Imbuement::attunement),
            Codec.BOOL.optionalFieldOf("receptive", false).forGetter(Imbuement::receptive),
            Codec.INT.optionalFieldOf("seed", 0).forGetter(Imbuement::seed)
    ).apply(instance, Imbuement::new));

    public static final StreamCodec<ByteBuf, Imbuement> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public double getTotalLifeforce() {
        double total = 0;
        for (double amount : lifeforce.values()) {
            total += amount;
        }
        return total;
    }

    public double getLifeforce(ResourceLocation type) {
        return lifeforce.getOrDefault(type, 0.0);
    }

    public double getAttunement(ResourceLocation domain) {
        return attunement.getOrDefault(domain, 0.0);
    }

    public boolean isEmpty() {
        return lifeforce.isEmpty() && attunement.isEmpty() && !receptive;
    }

    public Imbuement withLifeforce(ResourceLocation type, double added) {
        return new Imbuement(addCapped(lifeforce, type, added), attunement, receptive, seed);
    }

    public Imbuement withoutLifeforce() {
        return new Imbuement(Map.of(), attunement, false, seed);
    }

    public Imbuement withSeed(int seed) {
        return new Imbuement(lifeforce, attunement, receptive, seed);
    }

    public Imbuement asReceptive() {
        return new Imbuement(lifeforce, attunement, true, seed);
    }

    public Imbuement withAttunement(ResourceLocation domain, double added) {
        Map<ResourceLocation, Double> attuned = new HashMap<>(attunement);
        attuned.merge(domain, added, Double::sum);
        return new Imbuement(lifeforce, attuned, receptive, seed);
    }

    public Imbuement plus(Imbuement other) {
        Map<ResourceLocation, Double> summed = new HashMap<>(lifeforce);
        other.lifeforce.forEach((type, amount) -> summed.merge(type, amount, Double::sum));
        return new Imbuement(summed, attunement, receptive || other.receptive, seed != 0 ? seed : other.seed);
    }

    private static Map<ResourceLocation, Double> addCapped(Map<ResourceLocation, Double> amounts, ResourceLocation type, double added) {
        Map<ResourceLocation, Double> result = new HashMap<>(amounts);
        double amount = Math.min(LifeforceCaps.get(type), amounts.getOrDefault(type, 0.0) + added);
        if (amount > 0) {
            result.put(type, amount);
        } else {
            result.remove(type);
        }
        return result;
    }
}
