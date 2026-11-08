package ac.deneme.anticheat.utils.anticheat;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.api.event.events.DenemeJoinEvent;
import ac.deneme.anticheat.api.event.events.DenemeQuitEvent;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.platform.api.player.PlatformPlayer;
import ac.deneme.anticheat.platform.api.player.PlatformPlayerCache;
import ac.deneme.anticheat.utils.chunks.ChunkSectionCache;
import ac.deneme.anticheat.utils.reflection.GeyserUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private static final class Channels {
        static final DenemeJoinEvent.Channel JOIN = DenemeAPI.INSTANCE.getEventBus().get(DenemeJoinEvent.class);
        static final DenemeQuitEvent.Channel QUIT = DenemeAPI.INSTANCE.getEventBus().get(DenemeQuitEvent.class);
    }

    private final Set<User> exemptUsers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<User, DenemePlayer> playerDataMap = new ConcurrentHashMap<>();

    public boolean isExemptUser(@Nullable User user) {
        return user != null && exemptUsers.contains(user);
    }

    public void exemptUser(@Nullable User user) {
        if (user == null) return;
        exemptUsers.add(user);
    }

    public boolean clearExemptions(@Nullable User user) {
        if (user == null) return false;
        return exemptUsers.remove(user);
    }

    @Nullable
    public DenemePlayer getPlayer(final @NotNull UUID uuid) {

        Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(uuid);
        if (channel == null) return null;
        User user = PacketEvents.getAPI().getProtocolManager().getUser(channel);
        if (user == null) return null;
        return getPlayer(user);
    }

    @Nullable
    public DenemePlayer getPlayer(final @NotNull User user) {
        @Nullable DenemePlayer player = playerDataMap.get(user);
        if (player != null && player.platformPlayer != null && player.platformPlayer.isExternalPlayer())
            return null;
        return player;
    }

    public boolean shouldCheck(@NotNull User user) {
        if (isExemptUser(user)) return false;
        if (!ChannelHelper.isOpen(user.getChannel())) return false;

        if (user.getUUID() != null) {

            if (GeyserUtil.isBedrockPlayer(user.getUUID())) {
                exemptUser(user);
                return false;
            }

            DenemePlayer denemePlayer = DenemeAPI.INSTANCE.getPlayerDataManager().getPlayer(user);
            if (denemePlayer != null && denemePlayer.hasPermission("deneme.exempt")) {
                exemptUser(user);
                return false;
            }

            if (user.getUUID().toString().startsWith("00000000-0000-0000-0009")) {
                exemptUser(user);
                return false;
            }
        }

        return true;
    }

    public void addUser(final @NotNull User user) {
        if (shouldCheck(user)) {
            DenemePlayer player = new DenemePlayer(user);
            playerDataMap.put(user, player);
            Channels.JOIN.fire(player);
        }
    }

    public DenemePlayer remove(final @NotNull User user) {
        DenemePlayer removed = playerDataMap.remove(user);
        if (removed != null) ChunkSectionCache.releaseSharedSections(removed.compensatedWorld);
        return removed;
    }

    public void onDisconnect(User user) {
        DenemePlayer denemePlayer = remove(user);
        if (denemePlayer != null) Channels.QUIT.fire(denemePlayer);
        clearExemptions(user);

        UUID uuid = user.getProfile().getUUID();

        DenemeAPI.INSTANCE.getDataStoreLifecycle().liveWriteHooks()
                .onQuitFromUserDisconnect(user, denemePlayer, System.currentTimeMillis());
        if (uuid != null) {
            DenemeAPI.INSTANCE.getDataStoreLifecycle().playerToggleStore().evict(uuid);
        }

        if (uuid == null)
            return;

        PlatformPlayer quittingPlayer = PlatformPlayerCache.getInstance().getPlayer(uuid);
        if (quittingPlayer != null) {
            DenemeAPI.INSTANCE.getAlertManager().handlePlayerQuit(quittingPlayer);
        }

        DenemeAPI.INSTANCE.getSpectateManager().onQuit(uuid);

        DenemeAPI.INSTANCE.getPlatformPlayerFactory().invalidatePlayer(uuid);
    }

    public Collection<DenemePlayer> getEntries() {
        return playerDataMap.values();
    }

    public int size() {
        return playerDataMap.size();
    }
}
