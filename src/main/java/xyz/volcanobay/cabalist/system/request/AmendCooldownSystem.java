package xyz.volcanobay.cabalist.system.request;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cooldowns on someone who fails to amend a contract.
 */
public class AmendCooldownSystem {
    public static final AmendCooldownSystem INSTANCE = new AmendCooldownSystem();
    public static final long COOLDOWN_TICKS = 24000;

    private final Map<Key, Long> until = new HashMap<>();

    public record Key(UUID author, UUID contract) {
    }

    public void start(UUID author, UUID contract, long gameTime) {
        until.put(new Key(author, contract), gameTime + COOLDOWN_TICKS);
        RequestSystem.INSTANCE.markChanged();
    }

    public boolean isWaiting(UUID author, UUID contract, long gameTime) {
        return until.getOrDefault(new Key(author, contract), 0L) > gameTime;
    }

    public Map<Key, Long> getAll() {
        return until;
    }

    public void prune(long gameTime) {
        if (until.values().removeIf(end -> end <= gameTime)) {
            RequestSystem.INSTANCE.markChanged();
        }
    }

    public int clearFor(UUID author) {
        int before = until.size();
        until.keySet().removeIf(key -> key.author().equals(author));
        RequestSystem.INSTANCE.markChanged();
        return before - until.size();
    }

    public void clear() {
        until.clear();
    }

    public ListTag write() {
        ListTag list = new ListTag();
        until.forEach((key, end) -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("author", key.author());
            tag.putUUID("contract", key.contract());
            tag.putLong("until", end);
            list.add(tag);
        });
        return list;
    }

    public void read(ListTag list) {
        until.clear();
        for (Tag entry : list) {
            CompoundTag tag = (CompoundTag) entry;
            until.put(new Key(tag.getUUID("author"), tag.getUUID("contract")), tag.getLong("until"));
        }
    }
}
