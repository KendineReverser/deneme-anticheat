package ac.deneme.anticheat.manager.datastore;

import ac.deneme.anticheat.api.AbstractCheck;
import ac.deneme.anticheat.internal.storage.identity.PlayerIdentityService;
import ac.deneme.anticheat.platform.api.player.PlatformPlayer;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Live-write facade used when only the player-identity route is available.
 * History/session writes remain no-op, but join identity caching still works.
 */
final class IdentityLiveWriteHooks implements LiveWriteHooks {

    private final PlayerIdentityService identityService;

    IdentityLiveWriteHooks(@NotNull PlayerIdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void onJoin(
            @NotNull UUID uuid,
            @Nullable String name,
            long now,
            @NotNull SessionTracker.ClientMeta meta) {
        identityService.observe(uuid, name, now);
    }

    @Override public void onQuit(@NotNull UUID uuid, long now, @NotNull SessionTracker.ClientMeta meta) {}
    @Override public void observeBrand(@NotNull UUID uuid, long now, @NotNull SessionTracker.ClientMeta meta) {}

    @Override
    public void recordFlag(
            @NotNull UUID playerUuid,
            @NotNull AbstractCheck check,
            double vl,
            @Nullable String verbose,
            long now,
            @NotNull SessionTracker.ClientMeta meta) {
    }

    @Override
    public void recordFlagData(
            @NotNull UUID playerUuid,
            @NotNull AbstractCheck check,
            double vl,
            byte @Nullable [] verboseData,
            long now,
            @NotNull SessionTracker.ClientMeta meta) {
    }

    @Override
    public void onJoinFromUserLogin(@NotNull PlatformPlayer player, @NotNull User user, long now) {
        onJoin(player.getUniqueId(), player.getName(), now, SessionTracker.ClientMeta.empty());
    }

    @Override public void onQuitFromUserDisconnect(@NotNull User user, @Nullable DenemePlayer denemePlayer, long now) {}
    @Override public void observeBrandFromCheck(@NotNull DenemePlayer denemePlayer) {}

    @Override
    public void recordFlagFromCheck(
            @NotNull DenemePlayer player,
            @NotNull AbstractCheck check,
            double vl,
            @Nullable String verbose) {
    }

    @Override
    public void recordFlagDataFromCheck(
            @NotNull DenemePlayer player,
            @NotNull AbstractCheck check,
            double vl,
            byte @Nullable [] verboseData) {
    }
}
