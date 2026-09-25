package xyz.volcanobay.cabalist.system.spell;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.Stack;

public class SpellContextStack {
    private Stack<SpellContext> contextStack = new Stack<>();

    public SpellContextStack() {}

    public SpellContextStack pushCasterContext(Player player) {
        SpellContext spellContext = new SpellContext();

        Vec3 position = player.position();
        spellContext.locationContext.set(position.x,position.y,position.z);
        spellContext.target = player;
        spellContext.caster = player;
        spellContext.bearer = player;
        spellContext.level = player.level();

        contextStack.push(spellContext);
        return this;
    }

    public SpellContext context() {
        return contextStack.peek();
    }

    public SpellContext pop() {
        return contextStack.pop();
    }

    public static class SpellContext {
        protected Vector3d locationContext = new Vector3d();
        protected LivingEntity target = null;
        protected LivingEntity caster = null;
        protected LivingEntity bearer = null;
        protected Level level = null;


        private SpellContext() {

        }
    }
}
