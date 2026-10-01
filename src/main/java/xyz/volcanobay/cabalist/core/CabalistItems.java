package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.item.FocusItem;
import xyz.volcanobay.cabalist.item.SacrificialDaggerItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class CabalistItems {
    public static final RegistrationProvider<Item> ITEMS = RegistrationProvider.get(Registries.ITEM, Cabalist.MODID);
    private static final List<RegistryObject<? extends Item>> ITEM_ORDER = new ArrayList<>();

    public static final RegistryObject<SacrificialDaggerItem> SACRIFICIAL_DAGGER = registerItem("sacrificial_dagger",
            () -> new SacrificialDaggerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<FocusItem> STANDARD_WAND = registerItem("standard_wand",
            () -> new FocusItem(new Item.Properties().stacksTo(1), FocusItem.STANDARD));

    public static final RegistryObject<FocusItem> CREATIVE_STAFF = registerItem("creative_staff",
            () -> new FocusItem(new Item.Properties().stacksTo(1), FocusItem.CREATIVE));

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

    public static Collection<RegistryObject<Item>> getItems() {
        return ITEMS.getEntries();
    }

    public static void bootstrap() {
    }
}
