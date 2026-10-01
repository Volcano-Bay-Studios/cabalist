package xyz.volcanobay.cabalist.client.visibility;

import net.minecraft.core.BlockPos;
import xyz.volcanobay.cabalist.networking.packet.HiddenS2CPacket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// hidden tracks what its hiding from and the values of each.
public class ClientHidingSystem {
    public static final ClientHidingSystem INSTANCE = new ClientHidingSystem();
    private static final float EASE = 0.25f;
    private static final float SETTLED = 0.005f;

    private final Fades<Integer> entities = new Fades<>();
    private final Fades<Integer> visuals = new Fades<>();
    private final Fades<BlockPos> blocks = new Fades<>();
    private final Fades<Long> circles = new Fades<>();
    private final Fades<BlockCircle> blockCircles = new Fades<>();

    public void set(HiddenS2CPacket packet) {
        entities.retarget();
        visuals.retarget();
        blocks.retarget();
        circles.retarget();
        blockCircles.retarget();
        packet.entities().forEach(mark -> entities.target(mark.id(), mark.alpha()));
        packet.visuals().forEach(mark -> visuals.target(mark.id(), mark.alpha()));
        packet.blocks().forEach(mark -> blocks.target(mark.pos(), mark.alpha()));
        packet.circles().forEach(mark -> circles.target(circleKey(mark.host(), mark.wordsHash()), mark.alpha()));
        packet.blockCircles().forEach(mark -> blockCircles.target(new BlockCircle(mark.pos(), mark.wordsHash()), mark.alpha()));
    }

    public void tick() {
        entities.tick();
        visuals.tick();
        blocks.tick();
        circles.tick();
        blockCircles.tick();
    }

    public float getEntity(int id) {
        return entities.get(id);
    }

    public float getVisual(int id) {
        return visuals.get(id);
    }

    public float getBlock(BlockPos pos) {
        return blocks.get(pos);
    }

    public float getBlockCircle(BlockPos pos, String words) {
        return Math.min(blocks.get(pos), blockCircles.get(new BlockCircle(pos, words.hashCode())));
    }

    public float getCircle(int host, String words) {
        return Math.min(circles.get(circleKey(host, words.hashCode())), getEntity(host));
    }

    public boolean isEntityHidden(int id) {
        return isHidden(getEntity(id));
    }

    public static boolean isHidden(float alpha) {
        return alpha <= 0;
    }

    private static long circleKey(int host, int wordsHash) {
        return (long) host << 32 | wordsHash & 0xFFFFFFFFL;
    }

    public void clear() {
        entities.clear();
        visuals.clear();
        blocks.clear();
        circles.clear();
        blockCircles.clear();
    }

    private record BlockCircle(BlockPos pos, int wordsHash) {
    }

    private static class Fades<K> {
        private final Map<K, Float> targets = new HashMap<>();
        private final Map<K, Float> shown = new HashMap<>();

        private void retarget() {
            targets.replaceAll((key, alpha) -> 1f);
        }

        private void target(K key, float alpha) {
            targets.put(key, alpha);
            shown.putIfAbsent(key, alpha);
        }

        private float get(K key) {
            return shown.getOrDefault(key, 1f);
        }

        private void tick() {
            for (Iterator<Map.Entry<K, Float>> iterator = targets.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<K, Float> entry = iterator.next();
                float current = shown.getOrDefault(entry.getKey(), 1f);
                float next = current + (entry.getValue() - current) * EASE;
                if (Math.abs(entry.getValue() - next) < SETTLED) {
                    next = entry.getValue();
                }
                if (next >= 1 && entry.getValue() >= 1) {
                    iterator.remove();
                    shown.remove(entry.getKey());
                } else {
                    shown.put(entry.getKey(), next);
                }
            }
        }

        private void clear() {
            targets.clear();
            shown.clear();
        }
    }
}
