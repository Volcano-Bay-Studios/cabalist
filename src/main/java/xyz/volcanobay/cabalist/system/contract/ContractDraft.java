package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContractDraft {
    public static final int NAME_CIRCLE = -1;
    public static final int ADD_CIRCLE = -2;

    private final UUID id;
    private final UUID author;
    private final @Nullable UUID contractId;
    private @Nullable HostTarget createAt;
    private final List<Line> lines = new ArrayList<>();
    private String name;
    private @Nullable HostTarget rebind;
    private int nextLineId;

    public record Line(int id, String text, int origin) {
    }

    public ContractDraft(UUID author, Contract contract) {
        this(UUID.randomUUID(), author, contract.getUUID(), contract.getName());
        for (ContractState.Line line : ContractState.of(contract).lines()) {
            lines.add(new Line(nextLineId++, line.text(), line.origin()));
        }
    }

    public ContractDraft(UUID author, HostTarget createAt) {
        this(UUID.randomUUID(), author, null, "");
        this.createAt = createAt;
    }

    private ContractDraft(UUID id, UUID author, @Nullable UUID contractId, String name) {
        this.id = id;
        this.author = author;
        this.contractId = contractId;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAuthor() {
        return author;
    }

    public boolean isFor(Contract contract) {
        return contract.getUUID().equals(contractId);
    }

    public @Nullable Contract getContract() {
        return contractId == null ? null : ContractSystem.INSTANCE.getContract(contractId);
    }

    public @Nullable HostTarget getCreateAt() {
        return createAt;
    }

    public String getName() {
        return name;
    }

    public List<Line> getLines() {
        return lines;
    }

    public @Nullable HostTarget getRebind() {
        return rebind;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRebind(@Nullable HostTarget rebind) {
        this.rebind = rebind;
    }

    public boolean setLine(int lineId, String text) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).id() == lineId) {
                lines.set(i, new Line(lineId, text, lines.get(i).origin()));
                return true;
            }
        }
        return false;
    }

    public void addLine(String text) {
        lines.add(new Line(nextLineId++, text, ContractState.NEW_LINE));
    }

    public boolean removeLine(int lineId) {
        return lines.removeIf(line -> line.id() == lineId);
    }

    public ContractState toState() {
        List<ContractState.Line> stateLines = new ArrayList<>();
        for (Line line : lines) {
            stateLines.add(new ContractState.Line(line.text(), line.origin()));
        }
        return new ContractState(name, List.copyOf(stateLines), rebind);
    }

    public boolean isChanged(Line line, Contract contract) {
        List<String> current = contract.getIncantations();
        return line.origin() < 0 || line.origin() >= current.size() || !current.get(line.origin()).equals(line.text());
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putUUID("author", author);
        if (contractId != null) {
            tag.putUUID("contract", contractId);
        }
        if (createAt != null) {
            tag.put("create_at", createAt.write());
        }
        tag.putString("name", name);
        tag.putInt("next_line", nextLineId);
        ListTag lineList = new ListTag();
        for (Line line : lines) {
            CompoundTag lineTag = new CompoundTag();
            lineTag.putInt("id", line.id());
            lineTag.putString("text", line.text());
            lineTag.putInt("origin", line.origin());
            lineList.add(lineTag);
        }
        tag.put("lines", lineList);
        if (rebind != null) {
            tag.put("rebind", rebind.write());
        }
        return tag;
    }

    public static ContractDraft read(CompoundTag tag) {
        ContractDraft draft = new ContractDraft(tag.getUUID("id"), tag.getUUID("author"), tag.hasUUID("contract") ? tag.getUUID("contract") : null,
                tag.getString("name"));
        draft.createAt = tag.contains("create_at") ? HostTarget.read(tag.getCompound("create_at")) : null;
        draft.nextLineId = tag.getInt("next_line");
        for (Tag entry : tag.getList("lines", Tag.TAG_COMPOUND)) {
            CompoundTag lineTag = (CompoundTag) entry;
            draft.lines.add(new Line(lineTag.getInt("id"), lineTag.getString("text"), lineTag.getInt("origin")));
        }
        draft.rebind = tag.contains("rebind") ? HostTarget.read(tag.getCompound("rebind")) : null;
        return draft;
    }
}
