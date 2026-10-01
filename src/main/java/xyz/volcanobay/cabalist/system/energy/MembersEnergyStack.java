package xyz.volcanobay.cabalist.system.energy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.system.focus.Focus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

// A contract whose members pay the cost draws on their focuses, up to each focus's safe maximum.
public class MembersEnergyStack extends EnergyStack {
    private static final Map<UUID, Drawn> DRAWN = new HashMap<>();

    private final Supplier<? extends Iterable<UUID>> payers;

    private static class Drawn {
        private double amount;
        private long at;
    }

    public MembersEnergyStack(Supplier<? extends Iterable<UUID>> payers) {
        this.payers = payers;
    }

    private static boolean isEntropy(EnergyType type) {
        return type == CabalistEnergyTypes.ENTROPY.get();
    }

    @Override
    public double get(EnergyType type) {
        if (!isEntropy(type)) {
            return super.get(type);
        }
        double total = 0;
        for (ServerPlayer player : getOnlinePayers()) {
            Focus focus = Focus.carriedBy(player);
            if (focus != null) {
                total += getAvailable(player, focus);
            }
        }
        return total;
    }

    @Override
    public void give(EnergyType type, double amount) {
        if (!isEntropy(type)) {
            super.give(type, amount);
        }
    }

    @Override
    public void take(EnergyType type, double amount) {
        if (!isEntropy(type)) {
            super.take(type, amount);
            return;
        }
        double extracted = extract(type, amount);
        if (amount - extracted > 0) {
            addWorldPressure(amount - extracted);
        }
    }

    @Override
    public double extract(EnergyType type, double max) {
        if (!isEntropy(type)) {
            return super.extract(type, max);
        }
        List<ServerPlayer> online = getOnlinePayers();
        if (online.isEmpty() || max <= 0) {
            return 0;
        }
        double paid = 0;
        for (ServerPlayer player : online) {
            paid += pay(player, max / online.size());
        }
        for (ServerPlayer player : online) {
            if (paid >= max) {
                break;
            }
            paid += pay(player, max - paid);
        }
        return paid;
    }

    private static double pay(ServerPlayer player, double wanted) {
        Focus focus = Focus.carriedBy(player);
        if (focus == null || wanted <= 0) {
            return 0;
        }
        double paid = Math.min(wanted, getAvailable(player, focus));
        if (paid > 0) {
            getDrawn(player).amount += paid;
        }
        return Math.max(0, paid);
    }

    private static double getAvailable(ServerPlayer player, Focus focus) {
        double safe = focus.getSafeMaximum();
        Drawn drawn = getDrawn(player);
        long now = player.level().getGameTime();
        drawn.amount = Math.max(0, drawn.amount - safe * (now - drawn.at) / (CabalistConfig.CONTRACT_RECHARGE_SECONDS.get() * 20));
        drawn.at = now;
        return Math.max(0, safe - drawn.amount);
    }

    private static Drawn getDrawn(ServerPlayer player) {
        return DRAWN.computeIfAbsent(player.getUUID(), id -> {
            Drawn drawn = new Drawn();
            drawn.at = player.level().getGameTime();
            return drawn;
        });
    }

    public static void clear() {
        DRAWN.clear();
    }

    private List<ServerPlayer> getOnlinePayers() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        List<ServerPlayer> online = new ArrayList<>();
        if (server == null) {
            return online;
        }
        for (UUID id : payers.get()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null && player.isAlive()) {
                online.add(player);
            }
        }
        return online;
    }
}
