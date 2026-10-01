package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record ContractState(String name, List<Line> lines, @Nullable HostTarget rebind, boolean isDestroyed) {
    public static final int NEW_LINE = -1;

    public record Line(String text, int origin) {
    }

    public ContractState(String name, List<Line> lines, @Nullable HostTarget rebind) {
        this(name, lines, rebind, false);
    }

    public static ContractState of(Contract contract) {
        List<Line> lines = new ArrayList<>();
        List<String> incantations = contract.getIncantations();
        for (int i = 0; i < incantations.size(); i++) {
            lines.add(new Line(incantations.get(i), i));
        }
        return new ContractState(contract.getName(), lines, null);
    }

    public ContractState withLines(List<Line> newLines) {
        return new ContractState(name, List.copyOf(newLines), rebind);
    }

    public ContractState withName(String newName) {
        return new ContractState(newName, lines, rebind);
    }

    public ContractState withRebind(@Nullable HostTarget target) {
        return new ContractState(name, lines, target);
    }

    public ContractState destroyed() {
        return new ContractState(name, lines, rebind, true);
    }

    public ContractState withLine(Line line) {
        List<Line> newLines = new ArrayList<>(lines);
        newLines.add(line);
        return withLines(newLines);
    }

    public List<String> getTexts() {
        return lines.stream().map(Line::text).toList();
    }

    public List<String> describeChanges(Contract contract) {
        List<String> changes = new ArrayList<>();
        if (isDestroyed) {
            changes.add("destroy " + (contract.getName().isEmpty() ? "the contract" : contract.getName()));
            return changes;
        }
        if (!name.equals(contract.getName())) {
            changes.add("name: " + contract.getName() + " -> " + name);
        }
        List<String> current = contract.getIncantations();
        boolean[] kept = new boolean[current.size()];
        for (Line line : lines) {
            boolean isOriginal = line.origin() >= 0 && line.origin() < current.size();
            if (!isOriginal) {
                changes.add("+ " + line.text());
                continue;
            }
            kept[line.origin()] = true;
            if (!current.get(line.origin()).equals(line.text())) {
                changes.add("~ " + current.get(line.origin()) + " -> " + line.text());
            }
        }
        for (int i = 0; i < kept.length; i++) {
            if (!kept[i]) {
                changes.add("- " + current.get(i));
            }
        }
        if (rebind != null) {
            changes.add("moves to " + rebind.describe());
        }
        return changes;
    }

    public boolean isChangedFrom(Contract contract) {
        return !describeChanges(contract).isEmpty();
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        ListTag lineList = new ListTag();
        for (Line line : lines) {
            CompoundTag lineTag = new CompoundTag();
            lineTag.putString("text", line.text());
            lineTag.putInt("origin", line.origin());
            lineList.add(lineTag);
        }
        tag.put("lines", lineList);
        if (rebind != null) {
            tag.put("rebind", rebind.write());
        }
        tag.putBoolean("destroyed", isDestroyed);
        return tag;
    }

    public static ContractState read(CompoundTag tag) {
        List<Line> lines = new ArrayList<>();
        for (Tag entry : tag.getList("lines", Tag.TAG_COMPOUND)) {
            CompoundTag lineTag = (CompoundTag) entry;
            lines.add(new Line(lineTag.getString("text"), lineTag.getInt("origin")));
        }
        HostTarget rebind = tag.contains("rebind") ? HostTarget.read(tag.getCompound("rebind")) : null;
        return new ContractState(tag.getString("name"), List.copyOf(lines), rebind, tag.getBoolean("destroyed"));
    }
}
