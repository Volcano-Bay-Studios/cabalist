package xyz.volcanobay.cabalist.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.system.blood.BloodHelper;

public class SacrificialDaggerItem extends Item {
    private static final float SELF_DAMAGE = 2f;
    private static final int COOLDOWN_TICKS = 20;

    public SacrificialDaggerItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.hurt(player.damageSources().generic(), SELF_DAMAGE);
            BloodHelper.coverInBlood(stack, player, player, true);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
