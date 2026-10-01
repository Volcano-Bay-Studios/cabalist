package xyz.volcanobay.cabalist.system.render;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import foundry.veil.api.network.VeilPacketManager;
import xyz.volcanobay.cabalist.networking.packet.SpellVisualEndS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellShakeS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellVisualPaletteS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellVisualS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellVisualUpdateS2CPacket;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

import java.util.ArrayList;
import java.util.List;

/**
 *   visuals last until they are ended
 */
public class SpellVisuals {
    public static final int NO_VISUAL = -1;
    private static final double SEND_RANGE = 128;
    private static final double MIN_SHAKE_ENERGY = 0.5;

    private static int nextId;

    public static int send(Level level, FormShape shape, SpellClause clause) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return NO_VISUAL;
        }
        List<RenderSpec> specs = new ArrayList<>();
        collectSpecs(clause, specs);
        Spell spell = clause.getSpell();
        String words = spell == null ? "" : spell.getIncantation();
        int id = nextId++;
        VeilPacketManager.around(null, serverLevel, shape.start().x, shape.start().y, shape.start().z, SEND_RANGE)
                .sendPacket(new SpellVisualS2CPacket(id, shape, specs, words, spell == null ? Palette.EMPTY : getSpokenPalette(spell)));
        return id;
    }

    public static Palette getSpokenPalette(Spell spell) {
        if (!spell.getColors().isEmpty()) {
            return new Palette(spell.getColors());
        }
        List<Integer> colors = new ArrayList<>();
        for (SpellClause clause : spell.getClauses()) {
            for (int color : clause.getColors()) {
                if (!colors.contains(color)) {
                    colors.add(color);
                }
            }
        }
        return new Palette(List.copyOf(colors));
    }

    public static Palette getPalette(Spell spell) {
        List<RenderSpec> specs = new ArrayList<>();
        for (SpellClause clause : spell.getClauses()) {
            collectSpecs(clause, specs);
        }
        return getSpokenPalette(spell).or(Palette.of(RenderSpec.tintOf(specs)));
    }

    public static void collectSpecs(SpellClause clause, List<RenderSpec> out) {
        Spell spell = clause.getSpell();
        for (Aspect aspect : clause.getAspects()) {
            if (spell == null || clause.isBalance() || spell.isAspectActive(aspect)) {
                out.add(aspect.getRenderSpec(clause));
            }
        }
    }

    public static void update(Level level, int id, FormShape shape) {
        if (id == NO_VISUAL || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        VeilPacketManager.around(null, serverLevel, shape.start().x, shape.start().y, shape.start().z, SEND_RANGE)
                .sendPacket(new SpellVisualUpdateS2CPacket(id, shape));
    }

    public static void shake(Level level, Vector3d at, double energy) {
        if (energy < MIN_SHAKE_ENERGY || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        VeilPacketManager.around(null, serverLevel, at.x, at.y, at.z, SpellShakeS2CPacket.RANGE)
                .sendPacket(new SpellShakeS2CPacket(new Vec3(at.x, at.y, at.z), (float) energy));
    }

    public static void recolor(Level level, int id, Palette palette) {
        if (id != NO_VISUAL && level instanceof ServerLevel serverLevel) {
            VeilPacketManager.level(serverLevel).sendPacket(new SpellVisualPaletteS2CPacket(id, palette));
        }
    }

    public static void end(Level level, int id) {
        if (id == NO_VISUAL || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        VeilPacketManager.level(serverLevel).sendPacket(new SpellVisualEndS2CPacket(id));
    }
}
