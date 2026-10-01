package xyz.volcanobay.cabalist.persistent;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.contract.DraftSystem;
import xyz.volcanobay.cabalist.system.request.AmendCooldownSystem;
import xyz.volcanobay.cabalist.system.request.RequestSystem;

/**
 * Stores {@link ContractSystem} on the overworld. Always dirty, since contracts change without notice.
 */
public class ContractSavedData extends SavedData {
    public static final String ID = "cabalist_contracts";

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ContractSystem.INSTANCE.write(tag);
        tag.put("requests", RequestSystem.INSTANCE.write());
        tag.put("amend_cooldowns", AmendCooldownSystem.INSTANCE.write());
        tag.put("drafts", DraftSystem.INSTANCE.write());
        return tag;
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    private static ContractSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ContractSystem.INSTANCE.read(tag);
        RequestSystem.INSTANCE.read(tag.getList("requests", Tag.TAG_COMPOUND));
        AmendCooldownSystem.INSTANCE.read(tag.getList("amend_cooldowns", Tag.TAG_COMPOUND));
        DraftSystem.INSTANCE.read(tag.getList("drafts", Tag.TAG_COMPOUND));
        return new ContractSavedData();
    }

    public static ContractSavedData load(MinecraftServer server) {
        ContractSystem.INSTANCE.clear();
        RequestSystem.INSTANCE.clear();
        AmendCooldownSystem.INSTANCE.clear();
        DraftSystem.INSTANCE.clear();
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ContractSavedData::new, ContractSavedData::load), ID);
    }
}
