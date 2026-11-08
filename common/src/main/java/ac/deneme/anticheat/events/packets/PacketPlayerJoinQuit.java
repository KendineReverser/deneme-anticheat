package ac.deneme.anticheat.events.packets;

import ac.deneme.anticheat.DenemeAPI;
import ac.deneme.anticheat.manager.datastore.PlayerToggleStore;
import ac.deneme.anticheat.platform.api.player.PlatformPlayer;
import ac.deneme.anticheat.utils.anticheat.LogUtil;
import ac.deneme.anticheat.utils.functions.ObjBooleanConsumer;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class PacketPlayerJoinQuit extends PacketListenerAbstract {

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Login.Server.LOGIN_SUCCESS) return;

        event.getTasksAfterSend().add(() -> {
            DenemeAPI.INSTANCE.getPlayerDataManager().addUser(event.getUser());

            UUID uuid = event.getUser().getUUID();
            if (uuid != null) {
                DenemeAPI.INSTANCE.getDataStoreLifecycle().playerToggleStore().prefetch(uuid);
            }
        });
    }

    @Override
    public void onUserConnect(UserConnectEvent event) {

        if (event.getUser().getConnectionState() == ConnectionState.PLAY && !DenemeAPI.INSTANCE.getPlayerDataManager().isExemptUser(event.getUser())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {

        if (event.getUser() == null) return;

        Object nativePlayerObject = Objects.requireNonNull(event.getPlayer(), "nativePlayerObject");

        @NotNull PlatformPlayer platformPlayer = DenemeAPI.INSTANCE.getPlatformPlayerFactory().getFromNativePlayerType(nativePlayerObject);

        if (DenemeAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("debug-pipeline-on-join", false)) {
            LogUtil.info("Pipeline: " + ChannelHelper.pipelineHandlerNamesAsString(event.getUser().getChannel()));
        }

        PlayerToggleStore toggles = DenemeAPI.INSTANCE.getDataStoreLifecycle().playerToggleStore();
        applyToggle(platformPlayer, toggles, PlayerToggleStore.KEY_ALERTS,
                "deneme.alerts", "deneme.alerts.enable-on-join", "deneme.alerts.enable-on-join.silent",
                (p, silent) -> DenemeAPI.INSTANCE.getAlertManager().toggleAlerts(p, silent),
                (p, value) -> DenemeAPI.INSTANCE.getAlertManager().setAlertsEnabled(p, value, true));
        applyToggle(platformPlayer, toggles, PlayerToggleStore.KEY_VERBOSE,
                "deneme.verbose", "deneme.verbose.enable-on-join", "deneme.verbose.enable-on-join.silent",
                (p, silent) -> DenemeAPI.INSTANCE.getAlertManager().toggleVerbose(p, silent),
                (p, value) -> DenemeAPI.INSTANCE.getAlertManager().setVerboseEnabled(p, value, true));
        applyToggle(platformPlayer, toggles, PlayerToggleStore.KEY_BRANDS,
                "deneme.brand", "deneme.brand.enable-on-join", "deneme.brand.enable-on-join.silent",
                (p, silent) -> DenemeAPI.INSTANCE.getAlertManager().toggleBrands(p, silent),
                (p, value) -> DenemeAPI.INSTANCE.getAlertManager().setBrandsEnabled(p, value, true));

        if (platformPlayer.hasPermission("deneme.spectate") && DenemeAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("spectators.hide-regardless", false)) {
            DenemeAPI.INSTANCE.getSpectateManager().onLogin(platformPlayer.getUniqueId());
        }

        DenemeAPI.INSTANCE.getDataStoreLifecycle().liveWriteHooks()
                .onJoinFromUserLogin(platformPlayer, event.getUser(), System.currentTimeMillis());
    }

    /**
     * Apply one of the three persistable toggles at login time, honouring the
     * source-of-truth precedence:
     * <pre>
     *   persisted state (from prefetch) > permission-default > unset.
     * </pre>
     * If the toggle store is unavailable (datastore disabled / failed init),
     * fall through to the legacy permission-only behaviour so the plugin
     * keeps working without the v1 datastore.
     *
     * @param permTogglePath  permission required to USE the toggle at all
     * @param permEnableOnJoin permission whose default is "on at login"
     * @param permSilentJoin  permission that suppresses the toggle message
     * @param toggle          legacy fallback path (with on/off flip behaviour)
     * @param applySilent     applies a known boolean value silently — used
     *                        when persisted state dictates a specific value
     */
    private static void applyToggle(@NotNull PlatformPlayer platformPlayer,
                                    @NotNull PlayerToggleStore toggles,
                                    @NotNull String key,
                                    @NotNull String permTogglePath,
                                    @NotNull String permEnableOnJoin,
                                    @NotNull String permSilentJoin,
                                    @NotNull ObjBooleanConsumer<PlatformPlayer> toggle,
                                    @NotNull ObjBooleanConsumer<PlatformPlayer> applySilent) {
        if (!platformPlayer.hasPermission(permTogglePath)) return;

        UUID uuid = platformPlayer.getUniqueId();
        Boolean persisted = toggles.current(uuid, key);

        if (persisted != null) {

            applySilent.accept(platformPlayer, persisted);
            return;
        }

        boolean enableOnJoin = platformPlayer.hasPermission(permEnableOnJoin);
        boolean silent = platformPlayer.hasPermission(permSilentJoin);
        if (enableOnJoin) {
            toggle.accept(platformPlayer, silent);
            toggles.applyPermissionDefault(uuid, key, true);
        } else {

            toggles.applyPermissionDefault(uuid, key, false);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {

        DenemeAPI.INSTANCE.getPlayerDataManager().onDisconnect(event.getUser());
    }
}
