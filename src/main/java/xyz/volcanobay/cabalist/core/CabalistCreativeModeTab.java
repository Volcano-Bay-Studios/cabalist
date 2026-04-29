package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import xyz.volcanobay.cabalist.Cabalist;

public final class CabalistCreativeModeTab {
    public static final RegistrationProvider<CreativeModeTab> TABS = RegistrationProvider.get(Registries.CREATIVE_MODE_TAB, Cabalist.MODID);
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + Cabalist.MODID))
                    .icon(() -> new ItemStack(CabalistBlocks.OSCILISTONE.get()))
                    .displayItems(CabalistItems::fillTab)
                    .build());
    
    public static void bootstrap() {
    }
}
