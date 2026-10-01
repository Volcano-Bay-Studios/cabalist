package xyz.volcanobay.cabalist.system.spell;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.core.CabalistAttachments;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;

public class HangingSpellsData implements INBTSerializable<CompoundTag> {
    private final Entity entity;
    private ListTag unrestored = new ListTag();

    public HangingSpellsData(IAttachmentHolder holder) {
        this.entity = (Entity) holder;
    }

    public void restore() {
        if (unrestored.isEmpty()) {
            return;
        }
        EntitySubject host = EntitySubject.of(entity);
        for (int i = 0; i < unrestored.size(); i++) {
            PendingSpell pending = PendingSpell.read(unrestored.getCompound(i), host);
            if (!pending.isComplete()) {
                HangingSpellSystem.INSTANCE.add(pending);
            }
        }
        unrestored = new ListTag();
    }

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        ListTag spells = new ListTag();
        if (entity.hasData(CabalistAttachments.ENTITY_SUBJECT.get())) {
            for (PendingSpell pending : HangingSpellSystem.INSTANCE.get(EntitySubject.of(entity))) {
                spells.add(pending.write());
            }
        }
        spells.addAll(unrestored);
        CompoundTag tag = new CompoundTag();
        tag.put("spells", spells);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        unrestored = tag.getList("spells", Tag.TAG_COMPOUND);
    }
}
