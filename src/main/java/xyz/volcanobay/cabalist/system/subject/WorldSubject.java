package xyz.volcanobay.cabalist.system.subject;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.contract.WorldContractee;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

import java.util.UUID;

public class WorldSubject extends Subject {
    private final @Nullable Level level;

    public WorldSubject(@Nullable Level level) {
        this.level = level;
    }

    @Override
    public @Nullable Level getLevel() {
        return level;
    }

    @Override
    public void getPosition(Vector3d out) {
        out.zero();
    }

    @Override
    public void getFacing(Vector3d out) {
        out.set(0, -1, 0);
    }

    @Override
    public boolean receive(Aspect aspect, SpellClause clause, float magnitude) {
        return false;
    }

    @Override
    public UUID getUUID() {
        return WorldContractee.WORLD_ID;
    }

    @Override
    public Component getDisplayName() {
        return WorldContractee.INSTANCE.getDisplayName();
    }
}
