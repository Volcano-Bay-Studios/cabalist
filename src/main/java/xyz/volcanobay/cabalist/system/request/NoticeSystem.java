package xyz.volcanobay.cabalist.system.request;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.system.spell.Spell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Notification Circles.
 */
public class NoticeSystem {
    public static final NoticeSystem INSTANCE = new NoticeSystem();

    private final List<Notice> notices = new ArrayList<>();
    private final Map<UUID, Spell> boundTo = new HashMap<>();
    private final Set<UUID> dismissed = new HashSet<>();

    public record Notice(UUID id, UUID owner, String title, List<String> lines, int color, Vec3 about) {
    }

    public void add(UUID owner, String title, List<String> lines, int color, Vec3 about) {
        notices.add(new Notice(UUID.randomUUID(), owner, title, List.copyOf(lines), color, about));
        RequestSystem.INSTANCE.markChanged();
    }

    public void put(UUID id, UUID owner, String title, List<String> lines, int color, Vec3 about, Spell spell) {
        if (dismissed.contains(id)) {
            return;
        }
        Notice notice = new Notice(id, owner, title, List.copyOf(lines), color, about);
        boundTo.put(id, spell);
        for (int i = 0; i < notices.size(); i++) {
            if (notices.get(i).id().equals(id)) {
                if (!notices.get(i).equals(notice)) {
                    notices.set(i, notice);
                    RequestSystem.INSTANCE.markChanged();
                }
                return;
            }
        }
        notices.add(notice);
        RequestSystem.INSTANCE.markChanged();
    }

    public void tick() {
        for (Iterator<Map.Entry<UUID, Spell>> iterator = boundTo.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, Spell> entry = iterator.next();
            Spell spell = entry.getValue();
            if (spell.isDismissed() || !spell.isRunning()) {
                iterator.remove();
                dismissed.remove(entry.getKey());
                if (notices.removeIf(notice -> notice.id().equals(entry.getKey()))) {
                    RequestSystem.INSTANCE.markChanged();
                }
            }
        }
    }

    public @Nullable Notice get(UUID id) {
        for (Notice notice : notices) {
            if (notice.id().equals(id)) {
                return notice;
            }
        }
        return null;
    }

    public List<Notice> getAll() {
        return notices;
    }

    public void remove(Notice notice) {
        if (boundTo.containsKey(notice.id())) {
            dismissed.add(notice.id());
        }
        if (notices.remove(notice)) {
            RequestSystem.INSTANCE.markChanged();
        }
    }

    public void clear() {
        notices.clear();
        boundTo.clear();
        dismissed.clear();
    }
}
