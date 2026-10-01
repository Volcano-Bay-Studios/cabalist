package xyz.volcanobay.cabalist.content.spell.referent;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;
import xyz.volcanobay.cabalist.system.spell.SpellResolver;
import xyz.volcanobay.cabalist.system.spell.SpellRole;
import xyz.volcanobay.cabalist.system.spell.Word;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

// Quoted text, like invite "shop". It names a contract the caster is in first, then an online player
public class LiteralReferent extends SpellComponent {

    @Override
    public SpellRole getRole() {
        return SpellRole.REFERENT;
    }

    @Override
    public void resolve(Word word, SpellResolver resolver) {
        word.setSubject(findNamed(word.getPhrase(), word.getClause().getCaster()));
    }

    public static @Nullable Subject findNamed(String name, Subject caster) {
        for (Contract contract : ContractSystem.INSTANCE.getContracts()) {
            if (contract.getName().equalsIgnoreCase(name) && contract.canBeNamedBy(caster)) {
                return contract;
            }
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerPlayer player = server == null ? null : server.getPlayerList().getPlayerByName(name);
        return player == null ? null : EntitySubject.of(player);
    }
}
