package xyz.volcanobay.cabalist.system.subject;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractDraft;
import xyz.volcanobay.cabalist.system.contract.DraftSystem;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

import java.util.UUID;

/**
 * The subject of a contracts draft circles.
 */
public class DraftCircleSubject extends Subject {
    private final ContractDraft draft;
    private final int circle;
    private final Level level;
    private final Vec3 at;

    public DraftCircleSubject(ContractDraft draft, int circle, Level level, Vec3 at) {
        this.draft = draft;
        this.circle = circle;
        this.level = level;
        this.at = at;
    }

    public boolean canEdit(@Nullable UUID caster) {
        Contract contract = draft.getContract();
        return caster != null && (caster.equals(draft.getAuthor()) || contract != null && contract.isContractee(caster));
    }

    public boolean inscribe(String text, @Nullable UUID caster) {
        if (!canEdit(caster)) {
            return false;
        }
        boolean isChanged = switch (circle) {
            case ContractDraft.ADD_CIRCLE -> {
                draft.addLine(text);
                yield true;
            }
            case ContractDraft.NAME_CIRCLE -> {
                draft.setName(text);
                yield true;
            }
            default -> draft.setLine(circle, text);
        };
        return changed(isChanged);
    }

    public boolean rename(String name, @Nullable UUID caster) {
        if (!canEdit(caster) || circle != ContractDraft.NAME_CIRCLE) {
            return false;
        }
        draft.setName(name);
        return changed(true);
    }

    /**
     * Dispelling or burning the draft. Dispelling the name destroys the draft. Burning the name destroys the contract.
     */
    public boolean remove(boolean isDispel, Subject caster) {
        UUID id = caster.getUUID();
        if (circle != ContractDraft.NAME_CIRCLE) {
            return canEdit(id) && changed(draft.removeLine(circle));
        }
        if (!isDispel) {
            Contract contract = draft.getContract();
            return contract != null && RequestSystem.INSTANCE.destroyOrPropose(contract, caster);
        }
        if (!draft.getAuthor().equals(id)) {
            return false;
        }
        DraftSystem.INSTANCE.discard(draft);
        return true;
    }

    private static boolean changed(boolean isChanged) {
        if (isChanged) {
            DraftSystem.INSTANCE.markChanged();
        }
        return isChanged;
    }

    public ContractDraft getDraft() {
        return draft;
    }

    @Override
    public boolean isValid() {
        boolean isLine = circle >= 0;
        return DraftSystem.INSTANCE.get(draft.getId()) != null
                && (!isLine || draft.getLines().stream().anyMatch(line -> line.id() == circle));
    }

    @Override
    public @Nullable Level getLevel() {
        return level;
    }

    @Override
    public void getPosition(Vector3d out) {
        out.set(at.x, at.y, at.z);
    }

    @Override
    public void getFacing(Vector3d out) {
        out.set(0, 1, 0);
    }

    @Override
    public boolean receive(Aspect aspect, SpellClause clause, float magnitude) {
        return aspect.affectDraft(this, clause, magnitude);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Draft of ").append(draft.getName());
    }
}
