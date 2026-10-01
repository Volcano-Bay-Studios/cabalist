package xyz.volcanobay.cabalist.system.subject;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

/**
 * A block holding a contract: a rune or a contract slate.
 */
public class RuneSubject extends Subject {
    private final Level level;
    private final BlockPos pos;

    public RuneSubject(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public void getPosition(Vector3d out) {
        out.set(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
    }

    @Override
    public void getFacing(Vector3d out) {
        out.set(0, 1, 0);
    }

    @Override
    public boolean receive(Aspect aspect, SpellClause clause, float magnitude) {
        return aspect.affectPosition(level, pos, clause, magnitude);
    }

    @Override
    public Contract getBoundContract() {
        return ContractBlockEntity.getContractAt(level, pos);
    }

    @Override
    public boolean isValid() {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ContractBlockEntity;
    }

    @Override
    public Component getDisplayName() {
        return level.getBlockState(pos).getBlock().getName();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RuneSubject rune && rune.level == level && rune.pos.equals(pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }
}
