package xyz.volcanobay.cabalist.client.request;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.networking.packet.RequestsS2CPacket;

import java.util.List;

public class ClientRequestSystem {
    public static final ClientRequestSystem INSTANCE = new ClientRequestSystem();

    private final Int2ObjectMap<List<RequestsS2CPacket.Entry>> byEntity = new Int2ObjectOpenHashMap<>();
    private @Nullable ClientLevel level;

    public void set(int entityId, List<RequestsS2CPacket.Entry> requests) {
        checkLevel();
        if (requests.isEmpty()) {
            byEntity.remove(entityId);
        } else {
            byEntity.put(entityId, List.copyOf(requests));
        }
    }

    public List<RequestsS2CPacket.Entry> get(int entityId) {
        checkLevel();
        return byEntity.getOrDefault(entityId, List.of());
    }

    // Entity ids only mean something within one level.
    private void checkLevel() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != level) {
            byEntity.clear();
            level = current;
        }
    }
}
