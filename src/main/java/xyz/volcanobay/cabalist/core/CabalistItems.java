package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import xyz.volcanobay.cabalist.Cabalist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CabalistItems {
    public static final RegistrationProvider<Item> ITEMS = RegistrationProvider.get(Registries.ITEM, Cabalist.MODID);
    private static final List<RegistryObject<? extends Item>> ITEM_ORDER = new ArrayList<>();

    public static void fillTab(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        for (RegistryObject<? extends Item> object : ITEM_ORDER) {
            output.accept(object.get());
        }
    }

    public static <T extends Item> RegistryObject<T> registerItem(String name, Supplier<T> item) {
        RegistryObject<T> object = ITEMS.register(name, item);
        ITEM_ORDER.add(object);
        return object;
    }


    public static void bootstrap() {
    }
}
