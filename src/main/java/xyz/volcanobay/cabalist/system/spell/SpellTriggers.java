package xyz.volcanobay.cabalist.system.spell;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.ArrayList;

public class SpellTriggers {

    public static void raise(ResourceLocation type, Subject host, @Nullable Subject cause) {
        Level level = host.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        TriggerEvent event = new TriggerEvent(type, host, cause);
        long gameTime = level.getGameTime();
        HangingSpellSystem.INSTANCE.trigger(event, gameTime);
        Contract contract = host.getBoundContract();
        if (contract != null) {
            contract.bindHost(host);
            contract.onTrigger(event, gameTime);
        }
    }

    public static void raiseEverywhere(ResourceLocation type, Subject subject, @Nullable Subject cause) {
        Level level = subject.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        TriggerEvent event = new TriggerEvent(type, subject, cause);
        long gameTime = level.getGameTime();
        HangingSpellSystem.INSTANCE.triggerAll(event, gameTime);
        for (Contract contract : new ArrayList<>(ContractSystem.INSTANCE.getContracts())) {
            contract.onTrigger(event, gameTime);
        }
    }
}
