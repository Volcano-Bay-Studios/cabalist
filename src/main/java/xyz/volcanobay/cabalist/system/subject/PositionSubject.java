package xyz.volcanobay.cabalist.system.subject;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

public class PositionSubject extends Subject {
    private final Level level;
    private final Vector3d position = new Vector3d();
    private final Vector3d facing = new Vector3d();
    private final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

    public PositionSubject(Level level, Vector3dc position, Vector3dc facing) {
        this.level = level;
        this.position.set(position);
        this.facing.set(facing);
    }

    public static PositionSubject ofBlock(Level level, BlockPos pos) {
        return new PositionSubject(level, new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), new Vector3d());
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public void getPosition(Vector3d out) {
        out.set(position);
    }

    @Override
    public void getFacing(Vector3d out) {
        out.set(facing);
    }

    @Override
    public boolean receive(Aspect aspect, SpellClause clause, float magnitude) {
        blockPos.set(position.x, position.y, position.z);
        return aspect.affectPosition(level, blockPos, clause, magnitude);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(String.format("%.1f, %.1f, %.1f", position.x, position.y, position.z));
    }
}
