package xyz.volcanobay.cabalist.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xyz.volcanobay.cabalist.Cabalist;

import java.util.Map;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Unique
    private static int cabalist$lastSlot = -1;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/ClientHooks;shouldCauseReequipAnimation(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)Z"))
    private boolean cabalist$ignoreOwnComponents(ItemStack from, ItemStack to, int slot, Operation<Boolean> original) {
        boolean reequip = original.call(from, to, slot);
        boolean sameSlot = slot == -1 || slot == cabalist$lastSlot;
        if (slot != -1) {
            cabalist$lastSlot = slot;
        }
        return reequip && !(sameSlot && cabalist$differOnlyInOwnComponents(from, to));
    }

    @Unique
    private static boolean cabalist$differOnlyInOwnComponents(ItemStack from, ItemStack to) {
        if (from.isEmpty() || to.isEmpty() || from.getCount() != to.getCount() || !ItemStack.isSameItem(from, to)) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(cabalist$withoutOwnComponents(from), cabalist$withoutOwnComponents(to));
    }

    @Unique
    private static ItemStack cabalist$withoutOwnComponents(ItemStack stack) {
        ItemStack stripped = stack.copy();
        for (DataComponentType<?> type : stack.getComponentsPatch().entrySet().stream().map(Map.Entry::getKey).toList()) {
            ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (id != null && id.getNamespace().equals(Cabalist.MODID)) {
                stripped.remove(type);
            }
        }
        return stripped;
    }
}
