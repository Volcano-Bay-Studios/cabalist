package xyz.volcanobay.cabalist.system.request;

import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import xyz.volcanobay.cabalist.core.CabalistAttachments;

/**
 * Admins get consent requests (if enabled)
 */
public class WorldConsent {
    public static boolean isActive(ServerPlayer player) {
        return player.hasPermissions(Commands.LEVEL_GAMEMASTERS) && player.getData(CabalistAttachments.WORLD_CONSENT.get());
    }

    public static void set(ServerPlayer player, boolean active) {
        player.setData(CabalistAttachments.WORLD_CONSENT.get(), active);
        RequestSystem.INSTANCE.markChanged();
    }
}
