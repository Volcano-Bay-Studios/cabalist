package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record ContractAction(long time, @Nullable UUID actor, String action, String detail, String outcome) {

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("time", time);
        if (actor != null) {
            tag.putUUID("actor", actor);
        }
        tag.putString("action", action);
        tag.putString("detail", detail);
        tag.putString("outcome", outcome);
        return tag;
    }

    public static ContractAction read(CompoundTag tag) {
        return new ContractAction(tag.getLong("time"), tag.hasUUID("actor") ? tag.getUUID("actor") : null,
                tag.getString("action"), tag.getString("detail"), tag.getString("outcome"));
    }
}
