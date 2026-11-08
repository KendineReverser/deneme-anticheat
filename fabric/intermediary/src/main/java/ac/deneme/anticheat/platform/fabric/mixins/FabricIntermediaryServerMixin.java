package ac.deneme.anticheat.platform.fabric.mixins;

import ac.deneme.anticheat.platform.api.sender.Sender;
import ac.deneme.anticheat.platform.fabric.inject.FabricMinecraftServerHandle;
import ac.deneme.anticheat.platform.fabric.inject.FabricServerPlayerHandle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mixin(MinecraftServer.class)
@Implements(@Interface(iface = FabricMinecraftServerHandle.class, prefix = "deneme$", remap = Interface.Remap.NONE))
abstract class FabricIntermediaryServerMixin {

    public boolean deneme$isPlayerOnline(UUID uuid) {
        return ((MinecraftServer) (Object) this).getPlayerList().getPlayer(uuid) != null;
    }

    public FabricServerPlayerHandle deneme$playerByUuid(UUID uuid) {
        return (FabricServerPlayerHandle) ((MinecraftServer) (Object) this).getPlayerList().getPlayer(uuid);
    }

    public FabricServerPlayerHandle deneme$playerByName(String name) {
        return (FabricServerPlayerHandle) ((MinecraftServer) (Object) this).getPlayerList().getPlayerByName(name);
    }

    @SuppressWarnings("unchecked")
    public Collection<FabricServerPlayerHandle> deneme$onlinePlayers() {
        return (Collection<FabricServerPlayerHandle>) (Collection<?>) ((MinecraftServer) (Object) this).getPlayerList().getPlayers();
    }

    public Collection<UUID> deneme$savedPlayerUuids() {
        PlayerDataStorage storage = ((MinecraftServer) (Object) this).playerDataStorage;
        String[] files = storage.playerDir.list((dir, name) -> name.endsWith(".dat"));
        Set<UUID> uuids = new HashSet<>();
        if (files == null) return uuids;

        for (String file : files) {
            try {
                uuids.add(UUID.fromString(file.substring(0, file.length() - 4)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return uuids;
    }

    public int deneme$getTickCount() {
        return ((MinecraftServer) (Object) this).getTickCount();
    }

    public String deneme$getServerVersion() {
        return ((MinecraftServer) (Object) this).getServerVersion();
    }

    public Sender deneme$createCommandSender() {
        return (Sender) (Object) ((MinecraftServer) (Object) this).createCommandSourceStack();
    }

    public boolean deneme$usesAuthentication() {
        return ((MinecraftServer) (Object) this).usesAuthentication();
    }

    public boolean deneme$isRunning() {
        return ((MinecraftServer) (Object) this).isRunning();
    }

    public int deneme$getPlayerCount() {
        return ((MinecraftServer) (Object) this).getPlayerCount();
    }
}
