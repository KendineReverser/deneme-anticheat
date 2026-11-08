package ac.deneme.anticheat.platform.fabric.inject;

import ac.deneme.anticheat.platform.api.sender.Sender;

import java.util.Collection;
import java.util.UUID;

public interface FabricMinecraftServerHandle {
    boolean isPlayerOnline(UUID uuid);

    FabricServerPlayerHandle playerByUuid(UUID uuid);

    FabricServerPlayerHandle playerByName(String name);

    Collection<FabricServerPlayerHandle> onlinePlayers();

    Collection<UUID> savedPlayerUuids();

    int getTickCount();

    String getServerVersion();

    Sender createCommandSender();

    boolean usesAuthentication();

    boolean isRunning();

    int getPlayerCount();
}
