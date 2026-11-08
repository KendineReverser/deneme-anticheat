package ac.deneme.anticheat.manager.datastore;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.api.AbstractCheck;
import ac.deneme.anticheat.checks.impl.misc.ClientBrand;
import ac.deneme.anticheat.platform.api.player.PlatformPlayer;
import ac.deneme.anticheat.player.DenemePlayer;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Plugin-side facade that fans out player-lifecycle and flag events into the
 * data store. Datastore-disabled or init-failed paths get {@link #NOOP}, so
 * callers don't null-check.
 *
 * <p>The "from" convenience methods build all the per-call args (UUID, name,
 * timestamp, {@link SessionTracker.ClientMeta}) inside the impl. NOOP skips
 * the work entirely. Granular methods stay platform-neutral for tests +
 * tooling.
 */
public interface LiveWriteHooks {

    /** Player joined. Upsert identity (firstSeen=min / lastSeen=max) and open/extend a session. */
    void onJoin(@NotNull UUID uuid, @Nullable String name, long now, @NotNull SessionTracker.ClientMeta meta);

    /** Player disconnected. Flush the closing heartbeat on the session row + clear the in-memory tracker entry. */
    void onQuit(@NotNull UUID uuid, long now, @NotNull SessionTracker.ClientMeta meta);

    /**
     * Brand packet arrived. Re-issues a session upsert with the now-known
     * {@code clientBrand} field — the brand-channel plugin message can arrive
     * after PlayerJoinEvent, so the initial onJoin upsert may have left
     * client_brand null.
     */
    void observeBrand(@NotNull UUID uuid, long now, @NotNull SessionTracker.ClientMeta meta);

    /** Record a single flag with the full check object — pulls stable_key + description from the check. */
    void recordFlag(
            @NotNull UUID playerUuid,
            @NotNull AbstractCheck check,
            double vl,
            @Nullable String verbose,
            long now,
            @NotNull SessionTracker.ClientMeta meta);

    /** Record a single flag whose verbose payload is already encoded as opaque bytes. */
    void recordFlagData(
            @NotNull UUID playerUuid,
            @NotNull AbstractCheck check,
            double vl,
            byte @Nullable [] verboseData,
            long now,
            @NotNull SessionTracker.ClientMeta meta);

    /** Convenience for {@code PacketPlayerJoinQuit.onUserLogin}. Builds meta from the User + DenemePlayer internally. */
    void onJoinFromUserLogin(@NotNull PlatformPlayer player, @NotNull User user, long now);

    /** Convenience for {@code PlayerDataManager.onDisconnect}. Builds meta internally; tolerates null denemePlayer (early-disconnect race). */
    void onQuitFromUserDisconnect(@NotNull User user, @Nullable DenemePlayer denemePlayer, long now);

    /** Convenience for {@code ClientBrand.onPacketReceive}. Pulls UUID + builds meta from the DenemePlayer internally. */
    void observeBrandFromCheck(@NotNull DenemePlayer denemePlayer);

    /**
     * Convenience for {@code PunishmentManager.recordV1Flag}. Wraps the call
     * in a try/catch so call sites don't need their own — datastore failures
     * are warned once and swallowed (the legacy violation write already ran).
     */
    void recordFlagFromCheck(
            @NotNull DenemePlayer player,
            @NotNull AbstractCheck check,
            double vl,
            @Nullable String verbose);

    /** Binary-verbose counterpart to {@link #recordFlagFromCheck(DenemePlayer, AbstractCheck, double, String)}. */
    void recordFlagDataFromCheck(
            @NotNull DenemePlayer player,
            @NotNull AbstractCheck check,
            double vl,
            byte @Nullable [] verboseData);

    /**
     * Build the {@link SessionTracker.ClientMeta} stamped on session upserts
     * for {@code user}. Reads from the passed-in {@link DenemePlayer} (when
     * non-null) for the captured client brand. Public so tests and other
     * callers can mint a meta with the same shape the impl uses.
     */
    static @NotNull SessionTracker.ClientMeta clientMetaFor(
            @NotNull User user, @Nullable DenemePlayer gp) {
        String denemeVersion = DenemeAPI.INSTANCE.getExternalAPI().getDenemeVersion();
        int clientVersionPvn = user.getClientVersion().getProtocolVersion();
        String serverVersion = DenemeAPI.INSTANCE.getPlatformServer().getPlatformImplementationString();
        String brand = null;
        if (gp != null) {
            ClientBrand check = gp.checkManager.get(ClientBrand.class);
            if (check != null && check.isHasBrand()) brand = check.getBrand();
        }
        return new SessionTracker.ClientMeta(denemeVersion, brand, clientVersionPvn, serverVersion);
    }

    /**
     * No-op hooks returned by {@code DataStoreLifecycle.liveWriteHooks()}
     * when the v1 datastore is disabled or its init failed. Every method is
     * empty — the convenience overloads' arg-building is skipped too.
     */
    LiveWriteHooks NOOP = new LiveWriteHooks() {
        @Override public void onJoin(@NotNull UUID u, @Nullable String n, long t, @NotNull SessionTracker.ClientMeta m) {}
        @Override public void onQuit(@NotNull UUID u, long t, @NotNull SessionTracker.ClientMeta m) {}
        @Override public void observeBrand(@NotNull UUID u, long t, @NotNull SessionTracker.ClientMeta m) {}
        @Override public void recordFlag(@NotNull UUID u, @NotNull AbstractCheck c, double v, @Nullable String vb, long t, @NotNull SessionTracker.ClientMeta m) {}
        @Override public void recordFlagData(@NotNull UUID u, @NotNull AbstractCheck c, double v, byte @Nullable [] d, long t, @NotNull SessionTracker.ClientMeta m) {}
        @Override public void onJoinFromUserLogin(@NotNull PlatformPlayer p, @NotNull User u, long t) {}
        @Override public void onQuitFromUserDisconnect(@NotNull User u, @Nullable DenemePlayer g, long t) {}
        @Override public void observeBrandFromCheck(@NotNull DenemePlayer g) {}
        @Override public void recordFlagFromCheck(@NotNull DenemePlayer p, @NotNull AbstractCheck c, double v, @Nullable String vb) {}
        @Override public void recordFlagDataFromCheck(@NotNull DenemePlayer p, @NotNull AbstractCheck c, double v, byte @Nullable [] d) {}
    };
}
