package xyz.volcanobay.cabalist.system.contract;

import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistRequestStyles;
import xyz.volcanobay.cabalist.networking.packet.DraftsS2CPacket;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.ItemSubject;
import xyz.volcanobay.cabalist.system.subject.RuneSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.util.EntityHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DraftSystem {
    public static final DraftSystem INSTANCE = new DraftSystem();
    private static final int SYNC_INTERVAL_TICKS = 20;
    private static final double SYNC_RANGE = 48;

    private final List<ContractDraft> drafts = new ArrayList<>();
    private final Map<UUID, List<DraftsS2CPacket.Draft>> synced = new HashMap<>();
    private boolean isChanged = true;

    public List<ContractDraft> getDrafts() {
        return drafts;
    }

    public @Nullable ContractDraft get(UUID id) {
        for (ContractDraft draft : drafts) {
            if (draft.getId().equals(id)) {
                return draft;
            }
        }
        return null;
    }

    public ContractDraft open(UUID author, Contract contract) {
        for (ContractDraft draft : drafts) {
            if (draft.getAuthor().equals(author) && draft.isFor(contract)) {
                return draft;
            }
        }
        ContractDraft draft = new ContractDraft(author, contract);
        drafts.add(draft);
        contract.logAction(author, "draft", "", "opened");
        markChanged();
        return draft;
    }

    public void openNew(UUID author, HostTarget createAt) {
        drafts.add(new ContractDraft(author, createAt));
        markChanged();
    }

    public @Nullable ContractDraft getLatest(UUID author) {
        for (int i = drafts.size() - 1; i >= 0; i--) {
            if (drafts.get(i).getAuthor().equals(author)) {
                return drafts.get(i);
            }
        }
        return null;
    }

    public boolean setRebind(UUID author, HostTarget target) {
        ContractDraft draft = getLatest(author);
        if (draft == null) {
            return false;
        }
        draft.setRebind(target);
        markChanged();
        return true;
    }

    public boolean submit(ContractDraft draft) {
        if (draft.getCreateAt() != null) {
            return create(draft);
        }
        Contract contract = draft.getContract();
        if (contract == null || !RequestSystem.INSTANCE.submitAmend(contract, draft.getAuthor(), draft.toState())) {
            return false;
        }
        discard(draft);
        return true;
    }

    private boolean create(ContractDraft draft) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        HostTarget target = draft.getRebind() != null ? draft.getRebind() : draft.getCreateAt();
        if (server == null || target == null || draft.getLines().isEmpty()) {
            return false;
        }
        Contract contract = ContractSystem.INSTANCE.create(draft.getName(), draft.getAuthor());
        if (!target.place(contract, server)) {
            ContractSystem.INSTANCE.removeContract(contract);
            return false;
        }
        for (ContractDraft.Line line : draft.getLines()) {
            contract.addSpell(line.text(), draft.getAuthor());
        }
        discard(draft);
        return true;
    }

    public void discard(ContractDraft draft) {
        drafts.remove(draft);
        markChanged();
    }

    public void removeFor(Contract contract) {
        if (drafts.removeIf(draft -> draft.isFor(contract))) {
            markChanged();
        }
    }

    public void markChanged() {
        isChanged = true;
    }

    public void tick(MinecraftServer server) {
        if (!isChanged && server.getTickCount() % SYNC_INTERVAL_TICKS != 0) {
            return;
        }
        isChanged = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            List<DraftsS2CPacket.Draft> views = new ArrayList<>();
            for (ContractDraft draft : drafts) {
                DraftsS2CPacket.Draft view = toView(server, draft, player);
                if (view != null) {
                    views.add(view);
                }
            }
            if (!views.equals(synced.get(player.getUUID()))) {
                synced.put(player.getUUID(), views);
                VeilPacketManager.player(player).sendPacket(new DraftsS2CPacket(views));
            }
        }
        synced.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    private static DraftsS2CPacket.@Nullable Draft toView(MinecraftServer server, ContractDraft draft, ServerPlayer viewer) {
        Contract contract = draft.getContract();
        HostTarget createAt = draft.getCreateAt();
        if (contract == null && createAt == null) {
            return null;
        }
        Entity anchorEntity = null;
        BlockPos anchorBlock = null;
        Level level = null;
        Subject host = contract == null ? null : contract.getHost();
        if (createAt != null) {
            anchorEntity = createAt.entity() == null ? null : EntityHelper.find(server, createAt.entity());
            anchorBlock = createAt.pos();
            level = createAt.dimension() == null ? null : server.getLevel(createAt.dimension());
        } else if (host instanceof ItemSubject item && item.getHolder() instanceof EntitySubject holder) {
            anchorEntity = holder.getEntity();
        } else if (host instanceof RuneSubject rune) {
            anchorBlock = rune.getPos();
            level = rune.getLevel();
        } else {
            anchorEntity = server.getPlayerList().getPlayer(draft.getAuthor());
        }
        Vec3 anchor;
        if (anchorEntity != null) {
            level = anchorEntity.level();
            anchor = anchorEntity.position();
            anchorBlock = null;
        } else if (anchorBlock != null && level != null) {
            anchor = anchorBlock.getCenter();
        } else {
            return null;
        }
        if (level != viewer.level() || anchor.distanceTo(viewer.position()) > SYNC_RANGE) {
            return null;
        }
        boolean isContractee = contract != null && contract.isContractee(viewer.getUUID());
        boolean isAuthor = viewer.getUUID().equals(draft.getAuthor());
        List<DraftsS2CPacket.Circle> circles = new ArrayList<>();
        boolean isNameChanged = contract == null || !draft.getName().equals(contract.getName()) || draft.getRebind() != null;
        String name = draft.getRebind() == null ? draft.getName() : draft.getName() + " -> " + draft.getRebind().describe();
        circles.add(new DraftsS2CPacket.Circle(ContractDraft.NAME_CIRCLE, DraftsS2CPacket.KIND_NAME, name,
                color(isNameChanged ? "draft/edited" : "draft/name"), isContractee || isAuthor && isNameChanged));
        for (ContractDraft.Line line : draft.getLines()) {
            boolean isChanged = contract == null || draft.isChanged(line, contract);
            String style = line.origin() < 0 ? "draft/new" : isChanged ? "draft/edited" : "draft/line";
            circles.add(new DraftsS2CPacket.Circle(line.id(), DraftsS2CPacket.KIND_LINE, line.text(), color(style), isContractee || isAuthor && isChanged));
        }
        circles.add(new DraftsS2CPacket.Circle(ContractDraft.ADD_CIRCLE, DraftsS2CPacket.KIND_ADD, "", color("draft/add"), true));
        return new DraftsS2CPacket.Draft(draft.getId(), draft.getAuthor(), anchorEntity == null ? -1 : anchorEntity.getId(),
                Optional.ofNullable(anchorBlock), circles);
    }

    private static int color(String style) {
        return CabalistRequestStyles.get(style).color();
    }

    public void clear() {
        drafts.clear();
        synced.clear();
        markChanged();
    }

    public ListTag write() {
        ListTag list = new ListTag();
        for (ContractDraft draft : drafts) {
            list.add(draft.write());
        }
        return list;
    }

    public void read(ListTag list) {
        drafts.clear();
        for (Tag entry : list) {
            drafts.add(ContractDraft.read((CompoundTag) entry));
        }
        markChanged();
    }
}
