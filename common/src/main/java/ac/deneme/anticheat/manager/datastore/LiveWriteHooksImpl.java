package ac.deneme.anticheat.manager.datastore;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.api.AbstractCheck;
import ac.deneme.anticheat.api.storage.DataStore;
import ac.deneme.anticheat.api.storage.category.Categories;
import ac.deneme.anticheat.api.storage.model.VerboseFormat;
import ac.deneme.anticheat.internal.storage.checks.CheckRegistry;
import ac.deneme.anticheat.internal.storage.checks.StableKeyMapping;
import ac.deneme.anticheat.internal.storage.identity.PlayerIdentityService;
import ac.deneme.anticheat.platform.api.player.PlatformPlayer;
import ac.deneme.anticheat.player.DenemePlayer;
import ac.deneme.anticheat.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete {@link LiveWriteHooks}. Caches display-name → checkId locally so
 * the hot path skips the {@code synchronized intern(...)} call on
 * {@link CheckRegistry}.
 */
public final class LiveWriteHooksImpl implements LiveWriteHooks {

    private final DataStore store;
    private final PlayerIdentityService identityService;
    private final CheckRegistry checkRegistry;
    private final SessionTracker sessionTracker;
    private final BooleanSupplier writesAllowed;
    /** display name (lowercased) → checkId. Populated lazily via intern. */
    private final Map<String, Integer> checkIdCache = new ConcurrentHashMap<>();
    /** display-name-lowercase of checks we've already warned about. Prevents log spam. */
    private final Set<String> missingStableKeyLogged = ConcurrentHashMap.newKeySet();

    public LiveWriteHooksImpl(
            @NotNull DataStore store,
            @NotNull PlayerIdentityService identityService,
            @NotNull CheckRegistry checkRegistry,
            @NotNull SessionTracker sessionTracker) {
        this(store, identityService, checkRegistry, sessionTracker, () -> true);
    }

    LiveWriteHooksImpl(DataStore store, PlayerIdentityService identityService, CheckRegistry checkRegistry,
                       SessionTracker sessionTracker, BooleanSupplier writesAllowed) {
        this.writesAllowed = writesAllowed;
        this.store = store;
        this.identityService = identityService;
        this.checkRegistry = checkRegistry;
        this.sessionTracker = sessionTracker;
    }

    @Override
    public void onJoin(
            @NotNull UUID uuid,
            @Nullable String name,
            long now,
            @NotNull SessionTracker.ClientMeta meta) {
        identityService.observe(uuid, name, now);
        sessionTracker.observeActivity(uuid, now, meta);
    }

    @Override
    public void onQuit(@NotNull UUID uuid, long now, @NotNull SessionTracker.ClientMeta meta) {
        sessionTracker.close(uuid, now, meta);
    }

    @Override
    public void observeBrand(@NotNull UUID uuid, long now, @NotNull SessionTracker.ClientMeta meta) {
        sessionTracker.observeActivity(uuid, now, meta);
    }

    @Override
    public void recordFlag(
            @NotNull UUID playerUuid,
            @NotNull AbstractCheck check,
            double vl,
            @Nullable String verbose,
            long now,
            @NotNull SessionTracker.ClientMeta meta) {
        if (!writesAllowed.getAsBoolean()) return;
        UUID sessionId = sessionTracker.currentSessionId(playerUuid);
        if (sessionId == null) {

            sessionId = sessionTracker.observeActivity(playerUuid, now, meta);
        }
        final int checkId = resolveCheckId(check);
        final UUID sid = sessionId;
        store.submit(Categories.VIOLATION, e -> e
                .sessionId(sid)
                .playerUuid(playerUuid)
                .checkId(checkId)
                .vl(vl)
                .occurredEpochMs(now)
                .verbose(verbose)
                .verboseFormat(VerboseFormat.TEXT));
    }

    @Override
    public void recordFlagData(
            @NotNull UUID playerUuid,
            @NotNull AbstractCheck check,
            double vl,
            byte @Nullable [] verboseData,
            long now,
            @NotNull SessionTracker.ClientMeta meta) {
        if (!writesAllowed.getAsBoolean()) return;
        UUID sessionId = sessionTracker.currentSessionId(playerUuid);
        if (sessionId == null) {

            sessionId = sessionTracker.observeActivity(playerUuid, now, meta);
        }
        final int checkId = resolveCheckId(check);
        final UUID sid = sessionId;
        final byte[] payload = verboseData == null ? null : verboseData.clone();
        store.submit(Categories.VIOLATION, e -> e
                .sessionId(sid)
                .playerUuid(playerUuid)
                .checkId(checkId)
                .vl(vl)
                .occurredEpochMs(now)
                .verboseData(payload)
                .verboseFormat(VerboseFormat.STRUCTURED_V1));
    }

    @Override
    public void onJoinFromUserLogin(@NotNull PlatformPlayer player, @NotNull User user, long now) {
        DenemePlayer gp = DenemeAPI.INSTANCE.getPlayerDataManager().getPlayer(user);
        onJoin(player.getUniqueId(), player.getName(), now, LiveWriteHooks.clientMetaFor(user, gp));
    }

    @Override
    public void onQuitFromUserDisconnect(@NotNull User user, @Nullable DenemePlayer denemePlayer, long now) {
        UUID uuid = user.getUUID();
        if (uuid == null) return;
        onQuit(uuid, now, LiveWriteHooks.clientMetaFor(user, denemePlayer));
    }

    @Override
    public void observeBrandFromCheck(@NotNull DenemePlayer denemePlayer) {
        UUID uuid = denemePlayer.user.getUUID();
        if (uuid == null) return;
        observeBrand(uuid, System.currentTimeMillis(), LiveWriteHooks.clientMetaFor(denemePlayer.user, denemePlayer));
    }

    @Override
    public void recordFlagFromCheck(
            @NotNull DenemePlayer player,
            @NotNull AbstractCheck check,
            double vl,
            @Nullable String verbose) {
        try {
            recordFlag(player.uuid, check, vl, verbose, System.currentTimeMillis(), SessionTracker.ClientMeta.empty());
        } catch (RuntimeException e) {

            LogUtil.warn("v1 datastore recordFlag failed: " + e.getMessage());
        }
    }

    @Override
    public void recordFlagDataFromCheck(
            @NotNull DenemePlayer player,
            @NotNull AbstractCheck check,
            double vl,
            byte @Nullable [] verboseData) {
        try {
            recordFlagData(player.uuid, check, vl, verboseData, System.currentTimeMillis(), SessionTracker.ClientMeta.empty());
        } catch (RuntimeException e) {

            LogUtil.warn("v1 datastore recordFlag failed: " + e.getMessage());
        }
    }

    private int resolveCheckId(@NotNull AbstractCheck check) {
        String display = check.getCheckName();
        String key = display.toLowerCase(Locale.ROOT);
        Integer cached = checkIdCache.get(key);
        if (cached != null) return cached;

        String declaredStable = check.getStableKey();
        String stable;
        if (declaredStable != null && !declaredStable.isEmpty()) {
            stable = declaredStable;
        } else {

            if (missingStableKeyLogged.add(key)) {
                LogUtil.warn("[deneme-history] check " + display
                        + " has no stableKey declared; falling back to StableKeyMapping. "
                        + "Populate the @CheckData.stableKey / CheckInfo.stableKey field.");
            }
            stable = StableKeyMapping.stableKeyFor(display)
                    .orElse(StableKeyMapping.legacyFallback(display));
        }
        String description = check.getDescription();
        String introducedVersion = safePluginVersion();
        int id = checkRegistry.intern(stable, display, description, introducedVersion);
        checkIdCache.put(key, id);
        return id;
    }

    private static @Nullable String safePluginVersion() {
        try {
            return DenemeAPI.INSTANCE.getExternalAPI().getDenemeVersion();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
