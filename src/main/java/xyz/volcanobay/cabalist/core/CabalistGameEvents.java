package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.gameevent.GameEvent;
import xyz.volcanobay.cabalist.Cabalist;

public class CabalistGameEvents {
    private static final RegistrationProvider<GameEvent> GAME_EVENTS = RegistrationProvider.get(Registries.GAME_EVENT, Cabalist.MODID);

    public static final RegistryObject<GameEvent> BLOOD_SHED = GAME_EVENTS.register("blood_shed", () -> new GameEvent(16));

    public static Holder<GameEvent> getHolder(RegistryObject<GameEvent> event) {
        return BuiltInRegistries.GAME_EVENT.wrapAsHolder(event.get());
    }

    public static void bootstrap() {
    }
}
