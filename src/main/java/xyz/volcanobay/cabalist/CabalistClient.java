package xyz.volcanobay.cabalist;

import foundry.veil.platform.VeilEventPlatform;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import xyz.volcanobay.cabalist.client.CabalistClientEvents;

@Mod(value = Cabalist.MODID, dist = Dist.CLIENT)
public class CabalistClient {
    public CabalistClient(IEventBus modEventBus) {
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(CabalistClientEvents::onRenderLevel);
    }
}
